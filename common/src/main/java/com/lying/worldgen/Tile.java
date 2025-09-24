package com.lying.worldgen;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.lying.init.CDTiles;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class Tile
{
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
	
	public abstract void generate(BlockPos pos, ServerWorld world);
}
