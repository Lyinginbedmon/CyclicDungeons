package com.lying.grammar.content.trap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDTrapTypes;
import com.lying.item.WiringGunItem.WireMode;
import com.lying.reference.Reference;
import com.lying.utility.BlockPredicate;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class ModularTrap extends Trap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("modular");
	
	protected List<Module> modules = Lists.newArrayList();
	
	public ModularTrap(Identifier name)
	{
		super(name);
	}
	
	public static ModularTrap create() { return (ModularTrap)CDTrapTypes.get(ID).get(); }
	
	public ModularTrap module(Module moduleIn)
	{
		modules.removeIf(m -> m.name().equals(moduleIn.name()));
		modules.add(moduleIn);
		return this;
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject();
		JsonArray set = new JsonArray();
		modules.forEach(module -> set.add(Module.CODEC.encodeStart(ops, module).getOrThrow()));
		obj.add("Modules", set);
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonElement ele)
	{
		JsonObject obj = ele.getAsJsonObject();
		JsonArray set = obj.getAsJsonArray("Modules");
		for(int i=0; i<set.size(); i++)
			module(Module.CODEC.parse(ops, set.get(i)).getOrThrow());
		return this;
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme)
	{
		return super.isApplicableTo(room, meta, theme) && !modules.isEmpty() && modules.stream().map(Module::name).distinct().count() == modules.size();
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		final Random random = world.getRandom();
		
		// Sort modules by necessary order of placement
		modules.sort(Module.BUILD_ORDER);
		
		// Identify viable positions within room for all components
		Map<Identifier, List<BlockPos>> viabilityMap = new HashMap<>();
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
			modules.stream()
				.filter(m -> m.canExistAt(p, world))
				.map(Module::name)
				.forEach(m -> 
				{
					List<BlockPos> set = viabilityMap.getOrDefault(m, Lists.newArrayList());
					set.add(p.toImmutable());
					viabilityMap.put(m, set);
				})
		);
		
		// Build map of all components to be placed and where
		Map<Identifier, List<BlockPos>> componentMap = new HashMap<>();
		List<BlockPos> allComponents = Lists.newArrayList();
		for(Module component : modules)
		{
			final Identifier name = component.name();
			List<BlockPos> viable = viabilityMap.getOrDefault(name, Lists.newArrayList());
			
			viable.removeAll(allComponents);
			
			// Remove any positions that don't relational needs
			viable.removeIf(p -> !component.inValidRelations(p, componentMap));
			
			// Abort generation if a wiring component cannot be placed
			if(viable.isEmpty() && (component.isVitalComponent() || component.isWiringElement(modules)))
				return;
			
			List<BlockPos> set = Lists.newArrayList();
			for(int i=0; i<component.count(); i++)
			{
				if(viable.isEmpty())
					break;
				
				BlockPos pos = viable.size() == 1 ? viable.removeFirst() : viable.remove(random.nextInt(viable.size()));
				set.add(pos);
				allComponents.add(pos);
			}
			componentMap.put(name, set);
		}
		
		// Place all components in the world
		for(Module component : modules)
		{
			final Identifier name = component.name();
			List<BlockPos> points = componentMap.get(name);
			for(int i=0; i<component.count(); i++)
			{
				if(i >= points.size())
					break;
				
				component.placeAt(points.get(i), world);
			}
		}
		
		// Conduct wiring of wireable blocks
		for(Module component : modules.stream().filter(Module::needsWiring).toList())
		{
			final Identifier name = component.name();
			final List<BlockPos> receivers = componentMap.get(name);
			if(receivers.isEmpty())
				continue;
			
			final Consumer<BlockPos> wireTo = p -> receivers.forEach(receiver -> wireInputToBlock(receiver, world, p));
			
			// Wire all input blocks to each placed component
			for(Identifier id : component.inputs().get())
				componentMap.getOrDefault(id, Lists.newArrayList()).forEach(wireTo::accept);
		}
	}
	
	protected static void wireInputToBlock(BlockPos receiver, ServerWorld world, BlockPos input)
	{
		BlockState state = world.getBlockState(receiver);
		if(!(state.getBlock() instanceof IWireableBlock))
			return;
		
		IWireableBlock wireable = (IWireableBlock)state.getBlock();
		wireable.acceptWireTo(WireRecipient.SENSOR, input, WireMode.GLOBAL, receiver, world);
	}
	
	public static record Module(
			Identifier name, 
			int count, 
			BlockPredicate predicate, 
			Optional<List<Relation>> relations, 
			BlockState state, 
			Optional<List<Identifier>> inputs,
			Optional<Boolean> isVital)
	{
		public static final Codec<Module> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("id").forGetter(Module::name),
				Codec.INT.fieldOf("count").forGetter(Module::count),
				BlockPredicate.CODEC.fieldOf("condition").forGetter(Module::predicate),
				Relation.CODEC.listOf().optionalFieldOf("relations").forGetter(Module::relations),
				BlockState.CODEC.fieldOf("blockstate").forGetter(Module::state),
				Identifier.CODEC.listOf().optionalFieldOf("connections").forGetter(Module::inputs),
				Codec.BOOL.optionalFieldOf("vital").forGetter(Module::isVital)
				).apply(instance, (name,count,condition,relations,state,inputs,vital) -> 
				{
					Module.Builder builder = Builder.of(name);
					builder.count(count);
					builder.positioned(condition);
					builder.blockState(state);
					relations.ifPresent(set -> set.forEach(builder::relation));
					inputs.ifPresent(set -> set.forEach(builder::connection));
					if(vital.orElse(false))
						builder.markVital();
					return builder.build();
				}));
		public static final Comparator<Module> BUILD_ORDER = (a,b) -> 
		{
			// Case 0: Modules have no relations
			if(!a.hasRelations() && !b.hasRelations())
				return 0;
			// Case 1: Module A has relation to module B
			if(a.hasRelationTo(b.name()))
				return 1;
			// Case 2: Module B has relation to module A
			else if(b.hasRelationTo(a.name()))
				return -1;
			// Case 3: Modules have no relation to one-another
			return 0;
		};
		
		public boolean isVitalComponent() { return isVital.orElse(false); }
		
		public boolean canExistAt(BlockPos pos, ServerWorld world)
		{
			return predicate.applyTo(pos, world);
		}
		
		public boolean hasRelations() { return relations().isPresent() && !relations().get().isEmpty(); }
		
		public boolean hasRelationTo(Identifier name)
		{
			return relations().orElse(List.of()).stream().anyMatch(r -> r.name().equals(name));
		}
		
		public void placeAt(BlockPos pos, ServerWorld world)
		{
			world.setBlockState(pos, state());
		}
		
		public boolean isWireable() { return state.getBlock() instanceof IWireableBlock; }
		
		public boolean needsWiring() { return isWireable() && inputs.isPresent() && !inputs.get().isEmpty(); }
		
		public boolean isWiringElement(List<Module> modules)
		{
			return isWireable() && (!inputs.isEmpty() || modules.stream().map(Module::inputs).filter(Optional::isPresent).map(Optional::get).anyMatch(s -> s.contains(name)));
		}
		
		public boolean inValidRelations(BlockPos pos, Map<Identifier, List<BlockPos>> componentMap)
		{
			if(relations.isEmpty())
				return true;
			
			for(Relation neighbour : relations.get())
			{
				List<BlockPos> points = componentMap.getOrDefault(neighbour.name(), Lists.newArrayList());
				if(points.isEmpty() || points.stream().noneMatch(p -> neighbour.test(pos, p)))
					return false;
			}
			return true;
		}
		
		public static record Relation(Identifier name, Optional<BlockPos> offset, Optional<Integer> minDist, Optional<Integer> maxDist, Optional<Direction> side)
		{
			public static final Codec<Relation> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
					Identifier.CODEC.fieldOf("name").forGetter(Relation::name),
					BlockPos.CODEC.optionalFieldOf("offset").forGetter(Relation::offset),
					Codec.INT.optionalFieldOf("min_distance").forGetter(Relation::minDist),
					Codec.INT.optionalFieldOf("max_distance").forGetter(Relation::maxDist),
					Direction.CODEC.optionalFieldOf("side").forGetter(Relation::side)
					).apply(instance, Relation::new));
			
			public static Relation withOffset(Identifier name, BlockPos offset)
			{
				return new Relation(name, Optional.of(offset), Optional.empty(), Optional.empty(), Optional.empty());
			}
			
			public boolean test(BlockPos posA, BlockPos posB)
			{
				if(offset.isPresent() && posA.add(offset.get()).getManhattanDistance(posB) > 0)
					return false;
				if(side.isPresent() && Direction.fromVector(posB.subtract(posA), Direction.UP) != side.get())
					return false;
				if(minDist.isPresent() || maxDist.isPresent())
				{
					int dist = posA.getManhattanDistance(posB);
					if(dist < minDist.orElse(0) || dist > maxDist.orElse(Integer.MAX_VALUE))
						return false;
				}
				return true;
			}
		}
		
		public static class Builder
		{
			private final Identifier name;
			private int count = 1;
			private BlockPredicate positioning = null; 
			private List<Relation> relations = Lists.newArrayList(); 
			private BlockState state = Blocks.STONE.getDefaultState();
			private List<Identifier> inputs = Lists.newArrayList();
			private boolean isVital = false;
			
			protected Builder(Identifier nameIn)
			{
				name = nameIn;
			}
			
			public static Builder of(Identifier name)
			{
				return new Builder(name);
			}
			
			public Builder count(int val)
			{
				count = val;
				return this;
			}
			
			public Builder positioned(BlockPredicate predicate)
			{
				positioning = predicate;
				return this;
			}
			
			public Builder relation(Identifier id, BlockPos offset)
			{
				return relation(Relation.withOffset(id, offset));
			}
			
			public Builder relation(Relation relation)
			{
				relations.add(relation);
				return this;
			}
			
			public Builder blockState(BlockState stateIn)
			{
				state = stateIn;
				return this;
			}
			
			public Builder connection(Identifier... ids)
			{
				for(Identifier id : ids)
					connection(id);
				return this;
			}
			
			public Builder connection(Identifier id)
			{
				if(!inputs.contains(id))
					inputs.add(id);
				return this;
			}
			
			public Builder markVital()
			{
				isVital = true;
				return this;
			}
			
			public Module build()
			{
				return new Module(
						name, 
						count, 
						positioning == null ? BlockPredicate.Builder.create().build() : positioning, 
						relations.isEmpty() ? Optional.empty() : Optional.of(relations), 
						state, 
						inputs.isEmpty() ? Optional.empty() : Optional.of(inputs), 
						isVital ? Optional.of(isVital) : Optional.empty());
			}
		}
	}
}
