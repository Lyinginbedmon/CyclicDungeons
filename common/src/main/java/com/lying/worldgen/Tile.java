package com.lying.worldgen;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.BlueprintTileGrid.TileInstance;
import com.lying.init.CDLoggers;
import com.lying.init.CDTileTags.TileTag;
import com.lying.init.CDTiles;
import com.lying.utility.DebugLogger;
import com.mojang.serialization.Codec;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;

public abstract class Tile
{
	public static final Codec<Tile> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(Tile::registryName),
			Identifier.CODEC.listOf().optionalFieldOf("tags").forGetter(t -> ((Tile)t).tileTags.isEmpty() ? Optional.empty() : Optional.of(((Tile)t).tileTags)),
			GenStyle.CODEC.fieldOf("generation").forGetter(t -> ((Tile)t).type),
			TilePredicate.CODEC.fieldOf("conditions").forGetter(t -> t.predicate)
			// Rotation supplier
			)
			.apply(instance, (id,tags,type,predicate) -> 
			{
				Tile.Builder builder = Tile.Builder.of(predicate);
				return builder.build().apply(id);
			}));
	
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	public static final int TILE_SIZE = 2;
	public static final Vec2f TILE = new Vec2f(TILE_SIZE, TILE_SIZE);
	
	private final Identifier registryName;
	private final TilePredicate predicate;
	private final List<Identifier> tileTags = Lists.newArrayList();
	
	private final GenStyle type;
	private final RotationSupplier rotator;
	
	public Tile(Identifier id, List<Identifier> tagsIn, GenStyle style, TilePredicate predicateIn, RotationSupplier rotatorIn)
	{
		this.registryName = id;
		tileTags.addAll(tagsIn);
		this.type = style;
		this.predicate = predicateIn;
		this.rotator = rotatorIn;
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
	public final BlockRotation assignRotation(BlockPos pos, Function<BlockPos,Optional<Tile>> getter, Random rand)
	{
		return rotator.assignRotation(pos, getter, rand);
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
	
	@FunctionalInterface
	public static interface RotationSupplier
	{
		// FIXME Serialise RotationSupplier
		public static final Map<Direction, BlockRotation> faceToRotationMap = Map.of(
				Direction.NORTH, BlockRotation.NONE,
				Direction.EAST, BlockRotation.CLOCKWISE_90,
				Direction.SOUTH, BlockRotation.CLOCKWISE_180,
				Direction.WEST, BlockRotation.COUNTERCLOCKWISE_90
				);
		
		@NotNull
		public BlockRotation assignRotation(BlockPos pos, Function<BlockPos,Optional<Tile>> getter, Random rand);
		
		public static RotationSupplier none() { return (p,g,r) -> BlockRotation.NONE; }
		
		public static RotationSupplier random() { return (p,g,r) -> BlockRotation.values()[r.nextInt(BlockRotation.values().length)]; }
		
		public static RotationSupplier toFaceAdjacent(Predicate<Tile> predicate)
		{
			return toFaceAdjacent(predicate, none());
		}
		
		public static RotationSupplier againstBoundary(RotationSupplier fallback)
		{
			return (pos, getter, rand) -> 
			{
				for(Entry<Direction, BlockRotation> entry : faceToRotationMap.entrySet())
				{
					Optional<Tile> neighbour = getter.apply(pos.offset(entry.getKey()));
					if(neighbour.isEmpty())
						return entry.getValue();
				}
				
				return fallback.assignRotation(pos, getter, rand);
			};
		}
		
		public static RotationSupplier toFaceAdjacent(Predicate<Tile> predicate, RotationSupplier fallback)
		{
			final Map<Direction, BlockRotation> faceToRotationMap = Map.of(
					Direction.NORTH, BlockRotation.NONE,
					Direction.EAST, BlockRotation.CLOCKWISE_90,
					Direction.SOUTH, BlockRotation.CLOCKWISE_180,
					Direction.WEST, BlockRotation.COUNTERCLOCKWISE_90
					);
			
			return (pos, getter, rand) -> 
			{
				for(Entry<Direction, BlockRotation> entry : faceToRotationMap.entrySet())
				{
					Optional<Tile> neighbour = getter.apply(pos.offset(entry.getKey()));
					if(neighbour.isPresent() && predicate.test(neighbour.get()))
						return entry.getValue();
				}
				
				return fallback.assignRotation(pos, getter, rand);
			};
		}
	}
	
	public static class Builder
	{
		private final TilePredicate predicate;
		
		private GenStyle style = GenStyle.FLAG;
		private BlockState[] blockStates = new BlockState[0];
		private RotationSupplier rotationFunc = (p,g,r) -> BlockRotation.NONE;
		private List<Identifier> tileTags = Lists.newArrayList();
		
		private Builder(TilePredicate predicateIn)
		{
			predicate = predicateIn;
		}
		
		public static Builder of(TilePredicate predicate) { return new Builder(predicate); }
		
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
		
		public Builder asBlock(BlockState... states)
		{
			style = GenStyle.BLOCK;
			blockStates = states;
			return this;
		}
		
		public Builder asAir()
		{
			style = GenStyle.BLOCK;
			blockStates = new BlockState[] { Blocks.AIR.getDefaultState() };
			return this;
		}
		
		public Builder asStructure()
		{
			style = GenStyle.STRUCTURE;
			return this;
		}
		
		public Builder freeRotation()
		{
			return withRotation(RotationSupplier.random());
		}
		
		public Builder noRotation()
		{
			return withRotation((p,g,r) -> BlockRotation.NONE);
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
					return id -> new Tile(id, tileTags, style, predicate, RotationSupplier.none())
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world) { }
					};
				case BLOCK:
					return id -> new Tile(id, tileTags, style, predicate, RotationSupplier.none())
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world)
						{
							final int sc = Tile.TILE_SIZE - 1;
							BlockPos.Mutable.iterate(pos, pos.add(sc, sc, sc)).forEach(p -> Tile.tryPlace(blockStates[world.random.nextInt(blockStates.length)], p, world));
						}
					};
				case STRUCTURE:
					return id -> new Tile(id, tileTags, style, predicate, rotationFunc)
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
