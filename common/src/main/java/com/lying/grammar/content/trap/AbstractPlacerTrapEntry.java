package com.lying.grammar.content.trap;

import java.util.List;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.data.CDTags;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.init.CDLoggers;
import com.lying.worldgen.tile.Tile;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Describes a trap entry consisting of a small trap placed one or more times throughout a room after tile generation */
public abstract class AbstractPlacerTrapEntry extends TrapEntry
{
	public AbstractPlacerTrapEntry(Identifier idIn)
	{
		super(idIn);
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		int floorY = min.getY() + Tile.TILE_SIZE;
		
		// Find all doors
		List<BlockPos> avoiders = Lists.newArrayList();
		BlockPos.Mutable.iterate(min.withY(floorY), max.withY(floorY)).forEach(p -> 
		{
			BlockPos next = p.toImmutable();
			BlockState state = world.getBlockState(next);
			if(state.isIn(BlockTags.DOORS) || state.isIn(CDTags.PLACER_AVOID))
				avoiders.add(next);
		});
		
		// Find all viable places for trap
		final Predicate<BlockPos> avoidFunc = p -> avoiders.stream().noneMatch(a -> a.getManhattanDistance(p) >= minimumAvoiderDistance());
		List<BlockPos> viablePoints = Lists.newArrayList();
		BlockPos.Mutable.iterate(min.withY(floorY), max.withY(floorY)).forEach(p -> 
		{
			BlockPos next = p.toImmutable();
			if(avoidFunc.test(next) && isPosViableForTrap(next, world))
				viablePoints.add(next);
		});
		if(viablePoints.isEmpty())
			CDLoggers.WORLDGEN.warn("Failed to find any place for trap");
		
		// Select N places for traps
		Random rand = world.getRandom();
		List<BlockPos> traps = Lists.newArrayList();
		int i = getTrapCountForRoom(rand, meta.size());
		while(!viablePoints.isEmpty() && i-- > 0)
			traps.add(viablePoints.remove(rand.nextInt(viablePoints.size())));
		
		// Place traps
		while(!traps.isEmpty())
		{
			placeTrap(traps.remove(rand.nextInt(traps.size())), world, rand);
			
			// Traps may be chosen to be placed in areas that invalidate other traps, so re-evaluate the set each time one is placed
			traps.removeIf(p -> !isPosViableForTrap(p, world));
		}
	}
	
	/** Returns how far from any avoided blocks (such as doors) the traps should be placed */
	protected int minimumAvoiderDistance() { return 3; }
	
	/** Returns how many traps to place throughout the room */
	protected abstract int getTrapCountForRoom(Random rand, Vector2i roomSize);
	
	/** Returns true if the position is valid for trap placement */
	protected abstract boolean isPosViableForTrap(BlockPos pos, ServerWorld world);
	
	/** Generates the trap in the world */
	protected abstract void placeTrap(BlockPos pos, ServerWorld world, Random rand);
}
