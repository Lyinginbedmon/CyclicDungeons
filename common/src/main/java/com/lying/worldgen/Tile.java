package com.lying.worldgen;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.lying.init.CDTiles;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// FIXME Convert to placing structures instead of blocks
public abstract class Tile
{
	public static final int TILE_SIZE = 2;
	private final Identifier registryName;
	
	public Tile(Identifier id) { this.registryName = id; }
	
	public static Function<Identifier, Tile> of(BiPredicate<BlockPos,TileSet> exist, BiConsumer<BlockPos,ServerWorld> generator)
	{
		return id -> new Tile(id)
		{
			public boolean canExistAt(BlockPos pos, TileSet set) { return exist.test(pos, set); }
			
			public void generate(BlockPos pos, ServerWorld world) { generator.accept(pos, world); }
		};
	}
	
	public boolean equals(Object obj) { return obj instanceof Tile && ((Tile)obj).registryName().equals(this.registryName); }
	
	public Identifier registryName() { return this.registryName; }
	
	public final boolean isBlank() { return this.registryName.equals(CDTiles.BLANK.get().registryName()); }
	
	public abstract boolean canExistAt(BlockPos pos, TileSet set);
	
	// FIXME Include adjacent tiles for connecting pieces?
	public abstract void generate(BlockPos pos, ServerWorld world);
	
	public static void tryPlace(BlockState state, BlockPos pos, ServerWorld world)
	{
		if(!world.getBlockState(pos).isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
}
