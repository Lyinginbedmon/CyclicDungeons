package com.lying.grammar.content.trap.modular;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.utility.BlockPredicate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record Module(
		Identifier name, 
		int count, 
		BlockPredicate predicate, 
		Optional<List<Relation>> relations, 
		BlockState state, 
		Optional<ModuleWiring> inputs,
		Optional<Boolean> isVital)
{
	public static final Codec<Module> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(Module::name),
			Codec.INT.fieldOf("count").forGetter(Module::count),
			BlockPredicate.CODEC.fieldOf("condition").forGetter(Module::predicate),
			Relation.CODEC.listOf().optionalFieldOf("relations").forGetter(Module::relations),
			BlockState.CODEC.fieldOf("blockstate").forGetter(Module::state),
			ModuleWiring.CODEC.optionalFieldOf("connections").forGetter(Module::inputs),
			Codec.BOOL.optionalFieldOf("vital").forGetter(Module::isVital)
			).apply(instance, (name,count,condition,relations,state,inputs,vital) -> 
			{
				Module.Builder builder = Builder.of(name);
				builder.count(count);
				builder.positioned(condition);
				builder.blockState(state);
				relations.ifPresent(set -> set.forEach(builder::relation));
				inputs.ifPresent(builder::wiring);
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
		return isWireable() && (!inputs.isEmpty() || modules.stream().map(Module::inputs).filter(Optional::isPresent).map(Optional::get).anyMatch(s -> s.isWiringTarget(this)));
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
	
	public static record Relation(
			Identifier name, 
			Optional<BlockPos> offset, 
			Optional<Integer> minDist, 
			Optional<Integer> maxDist, 
			Optional<Direction> side)
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
		private ModuleWiring wiring = ModuleWiring.Simple.of(List.of());
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
		
		public Builder wiring(ModuleWiring var)
		{
			wiring = var;
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
					wiring.isEmpty() ? Optional.empty() : Optional.of(wiring), 
					isVital ? Optional.of(isVital) : Optional.empty());
		}
	}
}