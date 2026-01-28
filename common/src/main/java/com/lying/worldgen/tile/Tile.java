package com.lying.worldgen.tile;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.BlueprintTileGrid.TileInstance;
import com.lying.init.CDLoggers;
import com.lying.init.CDTileTags.TileTag;
import com.lying.init.CDTiles;
import com.lying.utility.DebugLogger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;

public abstract class Tile
{
	public static final Codec<Tile> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(Tile::registryName),
			TilePredicate.CODEC.fieldOf("conditions").forGetter(t -> t.predicate),
			Identifier.CODEC.listOf().optionalFieldOf("tags").forGetter(t -> ((Tile)t).tileTags.isEmpty() ? Optional.empty() : Optional.of(((Tile)t).tileTags)),
			GenStyle.CODEC.fieldOf("generation").forGetter(t -> ((Tile)t).type),
			BlockState.CODEC.listOf().optionalFieldOf("blocks").forGetter(t -> ((Tile)t).states),
			RotationSupplier.CODEC.fieldOf("rotation").forGetter(t -> t.rotator)
			)
			.apply(instance, (id,conditions,tags,generation,blocks,rotation) -> 
			{
				Tile.Builder builder = Tile.Builder.of(conditions);
				switch(generation)
				{
					case BLOCK:
						blocks.ifPresent(set -> builder.asBlock(set.toArray(new BlockState[0])));
						break;
					case STRUCTURE:
						builder.asStructure();
						break;
					default:
					case FLAG:
						break;
				}
				builder.withRotation(rotation);
				tags.ifPresent(set -> builder.tags(set));
				return builder.build().apply(id);
			}));
	
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	public static final int TILE_SIZE = 2;
	public static final Vec2f TILE = new Vec2f(TILE_SIZE, TILE_SIZE);
	
	private final Identifier registryName;
	private final TilePredicate predicate;
	private final List<Identifier> tileTags = Lists.newArrayList();
	
	private final GenStyle type;
	private final Optional<List<BlockState>> states;
	private final RotationSupplier rotator;
	
	public Tile(Identifier id, List<Identifier> tagsIn, GenStyle style, Optional<List<BlockState>> states, TilePredicate predicateIn, RotationSupplier rotatorIn)
	{
		this.registryName = id;
		tileTags.addAll(tagsIn);
		this.type = style;
		this.states = states;
		this.predicate = predicateIn;
		this.rotator = rotatorIn;
	}
	
	public JsonElement writeToJson(JsonOps ops)
	{
		return CODEC.encodeStart(ops, this).getOrThrow();
	}
	
	public static Tile readFromJson(JsonElement element, JsonOps ops)
	{
		return CODEC.parse(ops, element).getOrThrow();
	}
	
	public final boolean equals(Object obj) { return obj instanceof Tile && is((Tile)obj); }
	
	public final boolean is(Tile tile) { return tile.registryName().equals(registryName); }
	
	public final Identifier registryName() { return this.registryName; }
	
	public final boolean isIn(TileTag tag) { return tileTags.contains(tag.id()); }
	
	/** Shallow reference list of tile tags, each only containing this tile. Only used at startup. */
	public final List<TileTag> tags() { return tileTags.stream().map(id -> new TileTag(id).add(registryName)).toList(); }
	
	public final boolean isBlank() { return this.registryName.equals(CDTiles.BLANK.get().registryName()); }
	
	public final boolean isFlag() { return type == GenStyle.FLAG; }
	
	public final boolean canExistAt(BlockPos pos, BlueprintTileGrid set)
	{
		return predicate.test(this, pos, set);
	}
	
	/** Returns a valid rotation for an instance of this tile at the given coordinates in the tile set */
	@NotNull
	public final BlockRotation assignRotation(BlockPos pos, BlueprintTileGrid grid, Function<BlockPos,Optional<Tile>> func, Random rand)
	{
		return rotator.assignRotation(pos, grid, func, rand);
	}
	
	public abstract void generate(TileInstance inst, BlockPos pos, ServerWorld world);
	
	/** Helper function for placing blocks that avoids breaking anything that should be indestructible */
	public static void tryPlace(BlockState state, BlockPos pos, ServerWorld world)
	{
		if(!world.getBlockState(pos).isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	public static enum GenStyle implements StringIdentifiable
	{
		FLAG,
		AIR,
		BLOCK,
		STRUCTURE;
		
		@SuppressWarnings("deprecation")
		public static final EnumCodec<GenStyle> CODEC = StringIdentifiable.createCodec(GenStyle::values);
		
		public String asString() { return name().toLowerCase(); }
		
		public static GenStyle fromString(String name)
		{
			for(GenStyle style : values())
				if(style.asString().equalsIgnoreCase(name))
					return style;
			return FLAG;
		}
	}
	
	public static class Builder
	{
		private final TilePredicate predicate;
		
		private GenStyle style = GenStyle.FLAG;
		private Optional<List<BlockState>> states = Optional.empty();
		private RotationSupplier rotationFunc = RotationSupplier.NONE.get();
		private List<Identifier> tileTags = Lists.newArrayList();
		
		private Builder(TilePredicate predicateIn)
		{
			predicate = predicateIn;
		}
		
		public static Builder of(TilePredicate predicate) { return new Builder(predicate); }
		
		public Builder tags(List<Identifier> tags)
		{
			for(Identifier id : tags)
				if(!tileTags.contains(id))
					tileTags.add(id);
			return this;
		}
		
		public Builder tags(Identifier... tags)
		{
			for(Identifier id : tags)
				if(!tileTags.contains(id))
					tileTags.add(id);
			return this;
		}
		
		public Builder asFlag()
		{
			style = GenStyle.FLAG;
			return this;
		}
		
		public Builder asBlock(BlockState... statesIn)
		{
			style = GenStyle.BLOCK;
			states = Optional.of(Lists.newArrayList(statesIn));
			return this;
		}
		
		public Builder asAir()
		{
			style = GenStyle.AIR;
			return this;
		}
		
		public Builder asStructure()
		{
			style = GenStyle.STRUCTURE;
			return this;
		}
		
		public Builder freeRotation()
		{
			return withRotation(RotationSupplier.RANDOM.get());
		}
		
		public Builder noRotation()
		{
			return withRotation(RotationSupplier.NONE.get());
		}
		
		public Builder withRotation(RotationSupplier funcIn)
		{
			rotationFunc = funcIn;
			return this;
		}
		
		public Function<Identifier, Tile> build()
		{
			switch(style)
			{
				default:
				case FLAG:
				case AIR:
					return id -> new Tile(id, tileTags, style, states, predicate, RotationSupplier.NONE.get())
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world)
						{
							final int sc = Tile.TILE_SIZE - 1;
							BlockPos.Mutable.iterate(pos, pos.add(sc, sc, sc)).forEach(p -> Tile.tryPlace(Blocks.AIR.getDefaultState(), p, world));
						}
					};
				case BLOCK:
					return id -> new Tile(id, tileTags, style, states, predicate, RotationSupplier.NONE.get())
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world)
						{
							final int sc = Tile.TILE_SIZE - 1;
							List<BlockState> blocks = states.orElse(Lists.newArrayList(Blocks.AIR.getDefaultState()));
							if(blocks.isEmpty())
								return;
							BlockPos.Mutable.iterate(pos, pos.add(sc, sc, sc)).forEach(p -> Tile.tryPlace(blocks.get(world.random.nextInt(blocks.size())), p, world));
						}
					};
				case STRUCTURE:
					return id -> new Tile(id, tileTags, style, Optional.empty(), predicate, rotationFunc)
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world)
						{
							DynamicRegistryManager manager = world.getRegistryManager();
							Registry<StructurePool> registry = manager.getOrThrow(RegistryKeys.TEMPLATE_POOL);
							StructurePoolAliasLookup alias = StructurePoolAliasLookup.create(List.of(), pos, world.getSeed());
							RegistryKey<StructurePool> structureKey = inst.theme().getTilePool(inst.tile());
							Optional<StructurePool> poolOpt = Optional.of(structureKey).flatMap(key -> registry.getOptionalValue(alias.lookup(key)));
							if(poolOpt.isEmpty())
							{
								LOGGER.warn("Blank structure pool: {} for tile {} in theme {}", 
										structureKey.getValue().toString(), 
										inst.tile().registryName().toString(), 
										inst.theme().registryName().toString());
								return;
							}
							
							Random rand = Random.create(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
							StructureTemplateManager structureManager = world.getStructureTemplateManager();
							BlockRotation rotation = inst.rotation();
							
							// Adjust placement position based on rotation to keep the structure in the right spot overall
							BlockPos place = pos;
							if(rotation != BlockRotation.NONE)
								switch(rotation)
								{
									case CLOCKWISE_180:
										place = place.add(1, 0, 1);
										break;
									case CLOCKWISE_90:
										place = place.add(1, 0, 0);
										break;
									case COUNTERCLOCKWISE_90:
										place = place.add(0, 0, 1);
										break;
									default:
										break;
								}
							
							StructurePoolElement element = poolOpt.get().getRandomElement(rand);
							PoolStructurePiece piece = new PoolStructurePiece(
									structureManager,
									element,
									place,
									element.getGroundLevelDelta(),
									rotation,
									element.getBoundingBox(structureManager, place, rotation),
									StructureLiquidSettings.IGNORE_WATERLOGGING
									);
							piece.generate(world, world.getStructureAccessor(), world.getChunkManager().getChunkGenerator(), rand, BlockBox.create(pos, pos.add(1, 1, 1)), place, false);
						}
					};
			}
		}
	}
}
