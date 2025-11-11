package com.lying.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public class RedstoneActorBlock extends Block
{
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
	public RedstoneActorBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(POWERED, false));
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWERED);
	}
	
	protected boolean emitsRedstonePower(BlockState state) { return true; }
	
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction)
	{
		return state.get(POWERED) ? 15 : 0;
	}
}
