package com.lying.grammar.content.trap;

import java.util.List;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.block.CollisionSensorBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.SpikeTrapBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;
import com.lying.init.CDLoggers;

import net.minecraft.block.BlockState;
import net.minecraft.block.SideShapeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class BearTrapEntry extends AbstractPlacerTrapEntry
{
	public BearTrapEntry(Identifier idIn)
	{
		super(idIn);
	}
	
	protected int getTrapCountForRoom(Random rand, Vector2i roomSize) { return rand.nextBetween(3, 8); }
	
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world)
	{
		if(!canPlacePart(pos, world))
			return false;
		else
			return Direction.Type.HORIZONTAL.stream()
					.map(pos::offset)
					.anyMatch(p -> canPlacePart(p, world));
	}
	
	protected static boolean canPlacePart(BlockPos pos, ServerWorld world)
	{
		if(!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), Direction.UP, SideShapeType.FULL))
			return false;
		else
			return world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir();
	}
	
	protected void placeTrap(BlockPos pos, ServerWorld world, Random rand)
	{
		// Valid positions for spikes
		List<BlockPos> options = Lists.newArrayList();
		Direction.Type.HORIZONTAL.stream().map(pos::offset).filter(p -> canPlacePart(p, world)).forEach(options::add);
		if(options.isEmpty())
		{
			CDLoggers.WORLDGEN.warn("No valid positions found to place spike traps around sensor");
			return;
		}
		
		world.setBlockState(pos, CDBlocks.SENSOR_COLLISION.get().getDefaultState().with(CollisionSensorBlock.FACING, Direction.UP));
		final BlockState spikeState = CDBlocks.SPIKE_TRAP.get().getDefaultState().with(SpikeTrapBlock.FACING, Direction.UP);
		
		// Place spikes
		List<BlockPos> spikes = Lists.newArrayList();
		for(int i=rand.nextBetween(1, 2); i>0; i--)
		{
			BlockPos spikePos = options.remove(options.size() == 1 ? 0 : rand.nextInt(options.size()));
			world.setBlockState(spikePos, spikeState);
			spikes.add(spikePos);
			
			if(options.isEmpty())
				break;
		}
		
		// Wire spikes to sensor
		for(BlockPos spike : spikes)
			world.getBlockEntity(spike, CDBlockEntityTypes.SPIKE_TRAP.get()).ifPresent(t -> t.processWireConnection(pos, WireRecipient.SENSOR));
	}
}
