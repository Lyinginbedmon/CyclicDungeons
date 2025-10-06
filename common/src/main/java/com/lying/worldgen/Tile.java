package com.lying.worldgen;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.lying.blueprint.Blueprint;
import com.lying.init.CDTiles;
import com.lying.worldgen.TileSet.TileInstance;

import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public abstract class Tile
{
	public static final int TILE_SIZE = 2;
	
	private final Identifier registryName;
	private final TilePredicate predicate;
	
	private final GenStyle type;
	private final RotationSupplier rotator;
	
	public Tile(Identifier id, GenStyle style, TilePredicate predicateIn, RotationSupplier rotatorIn)
	{
		this.registryName = id;
		this.type = style;
		this.predicate = predicateIn;
		this.rotator = rotatorIn;
	}
	
	public boolean equals(Object obj) { return obj instanceof Tile && is((Tile)obj); }
	
	public boolean is(Tile tile) { return tile.registryName().equals(registryName); }
	
	public Identifier registryName() { return this.registryName; }
	
	public final boolean isBlank() { return this.registryName.equals(CDTiles.BLANK.get().registryName()); }
	
	public final boolean isFlag() { return type == GenStyle.FLAG; }
	
	public final boolean canExistAt(BlockPos pos, TileSet set) { return predicate.test(this, pos, set); }
	
	/** Returns a valid rotation for an instance of this tile at the given coordinates in the tile set */
	@NotNull
	public final BlockRotation assignRotation(BlockPos pos, Function<BlockPos,Tile> getter, Random rand) { return rotator.assignRotation(pos, getter, rand); }
	
	public abstract void generate(TileInstance inst, BlockPos pos, ServerWorld world);
	
	/** Helper function for placing blocks that avoids breaking anything that should be indestructible */
	public static void tryPlace(BlockState state, BlockPos pos, ServerWorld world)
	{
		if(!world.getBlockState(pos).isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	public static enum GenStyle
	{
		FLAG,
		BLOCK,
		STRUCTURE;
	}
	
	@FunctionalInterface
	public static interface RotationSupplier
	{
		@NotNull
		public BlockRotation assignRotation(BlockPos pos, Function<BlockPos,Tile> getter, Random rand);
		
		public static RotationSupplier none() { return (p,g,r) -> BlockRotation.NONE; }
		
		public static RotationSupplier random() { return (p,g,r) -> BlockRotation.values()[r.nextInt(BlockRotation.values().length)]; }
		
		public static RotationSupplier toFaceAdjacent(List<Supplier<Tile>> tiles)
		{
			return toFaceAdjacent(tiles, none());
		}
		
		// FIXME Resolve silent crashing during tile set finalisation
		public static RotationSupplier toFaceAdjacent(List<Supplier<Tile>> tiles, RotationSupplier fallback)
		{
			final Map<Direction, BlockRotation> faceToRotationMap = Map.of(
					Direction.NORTH, BlockRotation.NONE,
					Direction.EAST, BlockRotation.CLOCKWISE_90,
					Direction.SOUTH, BlockRotation.CLOCKWISE_180,
					Direction.WEST, BlockRotation.COUNTERCLOCKWISE_90
					);
			final Predicate<Tile> predicate = TilePredicate.tileAnyMatch(tiles);
			
			return (pos, getter, rand) -> 
			{
				for(Entry<Direction, BlockRotation> entry : faceToRotationMap.entrySet())
				{
					Tile neighbour = getter.apply(pos.offset(entry.getKey()));
					if(neighbour != null && predicate.test(neighbour))
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
		private RegistryKey<StructurePool> structureKey = null;
		private RotationSupplier rotationFunc = (p,g,r) -> BlockRotation.NONE;
		
		private Builder(TilePredicate predicateIn)
		{
			predicate = predicateIn;
		}
		
		public static Builder of(TilePredicate predicate) { return new Builder(predicate); }
		
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
		
		public Builder asStructure(RegistryKey<StructurePool> structureKeyIn)
		{
			style = GenStyle.STRUCTURE;
			structureKey = structureKeyIn;
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
					return id -> new Tile(id, style, predicate, RotationSupplier.none())
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world) { }
					};
				case BLOCK:
					return id -> new Tile(id, style, predicate, RotationSupplier.none())
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world)
						{
							final int sc = Tile.TILE_SIZE - 1;
							BlockPos.Mutable.iterate(pos, pos.add(sc, sc, sc)).forEach(p -> Tile.tryPlace(blockStates[world.random.nextInt(blockStates.length)], p, world));
						}
					};
				case STRUCTURE:
					return id -> new Tile(id, style, predicate, rotationFunc)
					{
						public void generate(TileInstance inst, BlockPos pos, ServerWorld world)
						{
							DynamicRegistryManager manager = world.getRegistryManager();
							Registry<StructurePool> registry = manager.getOrThrow(RegistryKeys.TEMPLATE_POOL);
							StructurePoolAliasLookup alias = StructurePoolAliasLookup.create(List.of(), pos, world.getSeed());
							Optional<StructurePool> poolOpt = Optional.of(structureKey).flatMap(key -> registry.getOptionalValue(alias.lookup(key)));
							if(poolOpt.isEmpty())
							{
								Blueprint.LOGGER.warn(" # Blank structure pool: {}", structureKey.getValue().toString());
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
