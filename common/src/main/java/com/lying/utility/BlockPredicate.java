package com.lying.utility;

import static com.lying.utility.CDUtils.listOrSolo;
import static com.lying.utility.CDUtils.orEmpty;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

/** Utility class for defining a predicate for BlockStates */
public class BlockPredicate extends AbstractMatcherPredicate<BlockState>
{
	private static final Codec<BlockPredicate> BASE_CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Registries.BLOCK.getCodec().listOf().optionalFieldOf("blocks").forGetter(p -> listOrSolo(p.blocks).getLeft()),
			Registries.BLOCK.getCodec().optionalFieldOf("block").forGetter(p -> listOrSolo(p.blocks).getRight()),
			Registries.FLUID.getCodec().listOf().optionalFieldOf("fluids").forGetter(p -> listOrSolo(p.fluids).getLeft()),
			Registries.FLUID.getCodec().optionalFieldOf("fluid").forGetter(p -> listOrSolo(p.fluids).getRight()),
			BlockState.CODEC.listOf().optionalFieldOf("states").forGetter(p -> listOrSolo(p.blockStates).getLeft()),
			BlockState.CODEC.optionalFieldOf("state").forGetter(p -> listOrSolo(p.blockStates).getRight()),
			TagKey.codec(RegistryKeys.BLOCK).listOf().optionalFieldOf("block_tags").forGetter(p -> listOrSolo(p.blockTags).getLeft()),
			TagKey.codec(RegistryKeys.BLOCK).optionalFieldOf("block_tag").forGetter(p -> listOrSolo(p.blockTags).getRight()),
			TagKey.codec(RegistryKeys.FLUID).listOf().optionalFieldOf("fluid_tags").forGetter(p -> listOrSolo(p.fluidTags).getLeft()),
			TagKey.codec(RegistryKeys.FLUID).optionalFieldOf("fluid_tag").forGetter(p -> listOrSolo(p.fluidTags).getRight()),
			Codec.STRING.listOf().optionalFieldOf("properties").forGetter(p -> listOrSolo(p.blockProperties).getLeft()),
			Codec.STRING.optionalFieldOf("property").forGetter(p -> listOrSolo(p.blockProperties).getRight()),
			PropertyMap.CODEC.listOf().optionalFieldOf("values").forGetter(p -> listOrSolo(p.blockValues).getLeft()),
			PropertyMap.CODEC.optionalFieldOf("value").forGetter(p -> listOrSolo(p.blockValues).getRight()),
			BlockFlags.CODEC.listOf().optionalFieldOf("flags").forGetter(p -> p.flags)
			)
				.apply(instance, (blockList, block, fluidList, fluid, stateList, state, blockTagList, blockTag, fluidTagList, fluidTag, propertyList, property, valueList, values, flags) -> 
				{
					Builder builder = Builder.create();
					blockList.ifPresent(l -> builder.addBlock(l.toArray(new Block[0])));
					block.ifPresent(builder::addBlock);
					
					fluidList.ifPresent(l -> builder.addFluid(l.toArray(new Fluid[0])));
					fluid.ifPresent(builder::addFluid);
					
					stateList.ifPresent(l -> builder.addBlockState(l.toArray(new BlockState[0])));
					state.ifPresent(builder::addBlockState);
					
					blockTagList.ifPresent(builder::addBlockTags);
					blockTag.ifPresent(builder::addBlockTag);
					
					fluidTagList.ifPresent(builder::addFluidTags);
					fluidTag.ifPresent(builder::addFluidTag);
					
					propertyList.ifPresent(l -> builder.addBlockProperty(l.toArray(new String[0])));
					property.ifPresent(builder::addBlockProperty);
					
					valueList.ifPresent(l -> builder.addBlockValues(l.toArray(new PropertyMap[0])));
					values.ifPresent(builder::addBlockValues);
					return builder.build();
				}));
	// FIXME Implement general codec for BlockPredicates
	
	protected final Optional<List<Block>> blocks;
	protected final Optional<List<Fluid>> fluids;
	protected final Optional<List<BlockState>> blockStates;
	protected final Optional<List<TagKey<Block>>> blockTags;
	protected final Optional<List<TagKey<Fluid>>> fluidTags;
	protected final Optional<List<String>> blockProperties;
	protected final Optional<List<PropertyMap>> blockValues;
	protected final Optional<List<BlockFlags>> flags;
	protected Optional<List<SubPredicate>> children;
	protected Optional<ChildLogic> childrenLogic;
	protected final boolean inverted;
	
	@SuppressWarnings("deprecation")
	protected BlockPredicate(
			boolean invertIn,
			Optional<List<Block>> blocksIn, 
			Optional<List<Fluid>> fluidsIn,
			Optional<List<BlockState>> statesIn, 
			Optional<List<TagKey<Block>>> blockTagsIn, 
			Optional<List<TagKey<Fluid>>> fluidTagsIn,
			Optional<List<String>> blockPropertiesIn, 
			Optional<List<PropertyMap>> blockValuesIn,
			Optional<List<BlockFlags>> flagsIn)
	{
		super(List.of(
				new ListMatcher<BlockState, Block>(blocksIn, (state, stream) -> stream.anyMatch(b -> state.isOf(b))), 
				new ListMatcher<BlockState, Fluid>(fluidsIn, (state, stream) -> stream.anyMatch(f -> state.getFluidState().getFluid().equals(f))),
				new ListMatcher<BlockState, BlockState>(statesIn, (state, stream) -> stream.anyMatch(s -> state.equals(s))), 
				new ListMatcher<BlockState, TagKey<Block>>(blockTagsIn, (state, stream) -> stream.anyMatch(t -> state.isIn(t))), 
				new ListMatcher<BlockState, TagKey<Fluid>>(fluidTagsIn, (state, stream) -> stream.anyMatch(t -> state.getFluidState().getFluid().isIn(t))),
				new ListMatcher<BlockState, String>(blockPropertiesIn, (state, stream) -> stream.allMatch(s -> state.getProperties().stream().anyMatch(p -> p.getName().equalsIgnoreCase(s)))),
				new ListMatcher<BlockState, PropertyMap>(blockValuesIn, (state, stream) -> stream.anyMatch(map -> map.matches(state)))
				));
		inverted = invertIn;
		blocks = blocksIn;
		fluids = fluidsIn;
		blockStates = statesIn;
		blockTags = blockTagsIn;
		fluidTags = fluidTagsIn;
		blockProperties = blockPropertiesIn;
		blockValues = blockValuesIn;
		flags = flagsIn;
	}
	
	public boolean applyTo(BlockPos pos, ServerWorld world)
	{
		boolean result = true;
		BlockState state = world.getBlockState(pos);
		
		// Block flags
		if(flags.isPresent() && !flags.get().stream().allMatch(f -> f.test(world, pos, state)))
			result = false;
		
		// Blockstate properties
		if(!isEmpty() && !apply(state))
			result = false;
		
		// Child predicates
		if(children.isPresent() && !childrenLogic.orElse(ChildLogic.AND).apply(children.get().stream(), pos, world))
			result = false;
		
		return result != inverted;
	}
	
	public JsonElement toJson()
	{
		JsonObject main = BASE_CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow().getAsJsonObject();
		children.ifPresent(set -> 
		{
			childrenLogic.ifPresent(l -> main.addProperty("children_logic", l.asString()));
			main.add("children", SubPredicate.CODEC.listOf().encodeStart(JsonOps.INSTANCE, set).getOrThrow());
		});
		return main;
	}
	
	public static BlockPredicate fromJson(JsonObject obj)
	{
		BlockPredicate main = BASE_CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow();
		if(obj.has("children"))
		{
			main.children = Optional.of(SubPredicate.CODEC.listOf().parse(JsonOps.INSTANCE, obj.get("children")).getOrThrow());
			main.childrenLogic = obj.has("children_logic") ? Optional.of(ChildLogic.fromString(obj.get("children_logic").getAsString())) : Optional.empty();
		}
		return main;
	}
	
	public static record SubPredicate(BlockPos offset, BlockPredicate predicate)
	{
		public static final Codec<SubPredicate> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("offset").forGetter(SubPredicate::offset),
				BlockPredicate.BASE_CODEC.fieldOf("condition").forGetter(SubPredicate::predicate)
				).apply(instance, SubPredicate::new));
		
		public boolean apply(BlockPos pos, ServerWorld world)
		{
			return predicate.applyTo(pos.add(offset), world);
		}
	}
	
	public static enum BlockFlags implements StringIdentifiable
	{
		SOLID((w,p,s) -> s.isSolidBlock(w, p)),
		REDSTONE((w,p,s) -> s.emitsRedstonePower()),
		OPAQUE((w,p,s) -> s.isOpaqueFullCube()),
		FLAMMABLE((w,p,s) -> s.isBurnable()),
		AIR((w,p,s) -> s.isAir()),
		PLAYER_ACCESSIBLE((w,p,s) -> 
		{
			if(!s.isAir())
				return false;
			else if(!w.getBlockState(p.up()).isAir())
				return false;
			
			Optional<BlockPos> trace = CDUtils.getCeilingAbove(p, (ServerWorld)w);
			if(trace.isEmpty())
				return false;
			
			BlockPos hit = trace.get();
			BlockState hitState = w.getBlockState(hit);
			return Block.isFaceFullSquare(hitState.getCollisionShape(w, hit), Direction.DOWN);
		});
		
		public static final Codec<BlockFlags> CODEC = StringIdentifiable.createCodec(BlockFlags::values);
		private final FlagTest predicate;
		
		private BlockFlags(FlagTest predicateIn)
		{
			predicate = predicateIn;
		}
		
		public boolean test(BlockView world, BlockPos pos, BlockState state)
		{
			return predicate.test(world, pos, state);
		}
		
		@FunctionalInterface
		private interface FlagTest
		{
			public boolean test(BlockView world, BlockPos pos, BlockState state);
		}
		
		public String asString() { return name().toLowerCase(); }
		
		@Nullable
		public static BlockFlags fromString(String name)
		{
			for(BlockFlags flag : values())
				if(flag.asString().equalsIgnoreCase(name))
					return flag;
			return null;
		}
	}
	
	public static enum ChildLogic implements StringIdentifiable
	{
		AND(Stream<SubPredicate>::allMatch),
		NAND(Stream<SubPredicate>::noneMatch),
		OR(Stream<SubPredicate>::anyMatch);
		
		private final BiFunction<Stream<SubPredicate>, Predicate<SubPredicate>, Boolean> validator;
		
		private ChildLogic(BiFunction<Stream<SubPredicate>, Predicate<SubPredicate>, Boolean> validatorIn)
		{
			validator = validatorIn;
		}
		
		public String asString() { return name().toLowerCase(); }
		
		public static ChildLogic fromString(String val)
		{
			for(ChildLogic logic : values())
				if(logic.asString().equalsIgnoreCase(val))
					return logic;
			return AND;
		}
		
		public boolean apply(Stream<SubPredicate> children, BlockPos pos, ServerWorld world)
		{
			return validator.apply(children, p -> p.apply(pos, world));
		}
	}
	
	public static class Builder
	{
		boolean inverted = false;
		List<Block> blocks = Lists.newArrayList();
		List<Fluid> fluids = Lists.newArrayList();
		List<BlockState> states = Lists.newArrayList();
		List<TagKey<Block>> blockTags = Lists.newArrayList();
		List<TagKey<Fluid>> fluidTags = Lists.newArrayList();
		List<String> blockProperties = Lists.newArrayList();
		List<PropertyMap> blockValues = Lists.newArrayList();
		List<BlockFlags> flags = Lists.newArrayList();
		List<SubPredicate> children = Lists.newArrayList();
		Optional<ChildLogic> childLogic = Optional.empty();
		
		protected Builder() { }
		
		public static Builder create() { return new Builder(); }
		
		public Builder invert()
		{
			inverted = true;
			return this;
		}
		
		public Builder addBlock(Block... blocks)
		{
			for(Block block : blocks)
			{
				this.blocks.removeIf(block::equals);
				this.blocks.add(block);
			}
			
			return this;
		}
		
		public Builder addFluid(Fluid... fluids)
		{
			for(Fluid fluid : fluids)
			{
				this.fluids.removeIf(fluid::equals);
				this.fluids.add(fluid);
			}
			return this;
		}
		
		public Builder addBlockState(BlockState... states)
		{
			for(BlockState state : states)
			{
				this.states.removeIf(state::equals);
				this.states.add(state);
			}
			
			return this;
		}
		
		public Builder addBlockTag(TagKey<Block> tags)
		{
			return addBlockTags(List.of(tags));
		}
		
		public Builder addBlockTags(List<TagKey<Block>> tagsIn)
		{
			tagsIn.forEach(tag -> 
			{
				this.blockTags.removeIf(tag::equals);
				this.blockTags.add(tag);
			});
			return this;
		}
		
		public Builder addFluidTag(TagKey<Fluid> tags)
		{
			return addFluidTags(List.of(tags));
		}
		
		public Builder addFluidTags(List<TagKey<Fluid>> tagsIn)
		{
			tagsIn.forEach(tag -> 
			{
				this.fluidTags.removeIf(tag::equals);
				this.fluidTags.add(tag);
			});
			return this;
		}
		
		public Builder addBlockProperty(String... values)
		{
			for(String value : values)
				if(!blockProperties.contains(value))
					blockProperties.add(value);
			return this;
		}
		
		public Builder addBlockValues(PropertyMap... values)
		{
			for(PropertyMap map : values)
				if(blockValues.stream().noneMatch(b -> PropertyMap.equals(b, map)))
					blockValues.add(map);
			return this;
		}
		
		public Builder addFlag(BlockFlags... flagsIn)
		{
			for(BlockFlags flag : flagsIn)
				flags.add(flag);
			return this;
		}
		
		public Builder child(SubPredicate childIn)
		{
			children.add(childIn);
			return this;
		}
		
		public Builder childLogic(ChildLogic logic)
		{
			childLogic = Optional.of(logic);
			return this;
		}
		
		public BlockPredicate build()
		{
			BlockPredicate main = new BlockPredicate(
					inverted,
					orEmpty(blocks), 
					orEmpty(fluids),
					orEmpty(states),
					orEmpty(blockTags),
					orEmpty(fluidTags),
					orEmpty(blockProperties),
					orEmpty(blockValues),
					orEmpty(flags));
			main.children = orEmpty(children);
			main.childrenLogic = childLogic;
			return main;
		}
	}
}