package com.lying.worldgen;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.lying.blueprint.Blueprint;
import com.lying.init.CDTiles;

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
import net.minecraft.util.math.random.Random;

public abstract class Tile
{
	public static final int TILE_SIZE = 2;
	
	private final Identifier registryName;
	private final TilePredicate predicate;
	
	public Tile(Identifier id, TilePredicate predicateIn)
	{
		this.registryName = id;
		predicate = predicateIn;
	}
	
	public static Function<Identifier, Tile> of(TilePredicate exist, BiConsumer<BlockPos,ServerWorld> generator)
	{
		return id -> new Tile(id, exist)
		{
			public void generate(BlockPos pos, ServerWorld world) { generator.accept(pos, world); }
		};
	}
	
	public boolean equals(Object obj) { return obj instanceof Tile && is((Tile)obj); }
	
	public boolean is(Tile tile)
	{
		return tile.registryName().equals(registryName);
	}
	
	public Identifier registryName() { return this.registryName; }
	
	public final boolean isBlank() { return this.registryName.equals(CDTiles.BLANK.get().registryName()); }
	
	public final boolean canExistAt(BlockPos pos, TileSet set)
	{
		return predicate.canExistAt(this, pos, set);
	}
	
	// FIXME Include adjacent tiles for connecting pieces?
	public abstract void generate(BlockPos pos, ServerWorld world);
	
	public static void tryPlace(BlockState state, BlockPos pos, ServerWorld world)
	{
		if(!world.getBlockState(pos).isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	public static class StructureTile extends Tile
	{
		private final RegistryKey<StructurePool> poolKey;
		private final boolean freeRotation;
		
		public StructureTile(Identifier id, TilePredicate predicate, RegistryKey<StructurePool> keyIn, boolean rotationIn)
		{
			super(id, predicate);
			poolKey = keyIn;
			freeRotation = rotationIn;
		}
		
		public static Function<Identifier, Tile> of(TilePredicate exist, RegistryKey<StructurePool> poolKey)
		{
			return of(exist, poolKey, false);
		}
		
		public static Function<Identifier, Tile> of(TilePredicate exist, RegistryKey<StructurePool> poolKey, boolean freelyRotate)
		{
			return id -> new StructureTile(id, exist, poolKey, freelyRotate);
		}
		
		public void generate(BlockPos pos, ServerWorld world)
		{
			DynamicRegistryManager manager = world.getRegistryManager();
			Registry<StructurePool> registry = manager.getOrThrow(RegistryKeys.TEMPLATE_POOL);
			StructurePoolAliasLookup alias = StructurePoolAliasLookup.create(List.of(), pos, world.getSeed());
			Optional<StructurePool> poolOpt = Optional.of(poolKey).flatMap(key -> registry.getOptionalValue(alias.lookup(key)));
			if(poolOpt.isEmpty())
			{
				Blueprint.LOGGER.warn(" # Blank structure pool: {}", poolKey.getValue().toString());
				return;
			}
			
			Random rand = Random.create(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
			StructureTemplateManager structureManager = world.getStructureTemplateManager();
			BlockRotation rotation = BlockRotation.NONE;
			BlockPos place = pos;
			if(freeRotation)
			{
				rotation = BlockRotation.values()[rand.nextInt(BlockRotation.values().length)];
				rotation = BlockRotation.COUNTERCLOCKWISE_90;
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
					case NONE:
						break;
				}
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
	}
}
