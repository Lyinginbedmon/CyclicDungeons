package com.lying.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

public class RedstoneSensorBlock extends AbstractTrapSensorBlock
{
	public RedstoneSensorBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(POWERED, false));
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWERED);
	}
	
	protected boolean emitsRedstonePower(BlockState state) { return true; }
	
	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify)
	{
		if(!world.isClient)
		{
			boolean status = (Boolean)state.get(POWERED);
			if (status != world.isReceivingRedstonePower(pos))
				if(status)
					world.scheduleBlockTick(pos, this, 1);
				else
					world.setBlockState(pos, state.cycle(POWERED), 2);
		}
	}
	
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random)
	{
		if ((Boolean)state.get(POWERED) && !world.isReceivingRedstonePower(pos))
			world.setBlockState(pos, state.cycle(POWERED), 2);
	}
	
	public boolean isActive(BlockPos pos, World world)
	{
		return world.getBlockState(pos).get(POWERED);
	}
}
