package com.lying.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class RedstoneActorBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<RedstoneActorBlock> CODEC = RedstoneActorBlock.createCodec(RedstoneActorBlock::new);
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
	public RedstoneActorBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(POWERED, false));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec()
	{
		return CODEC;
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
	
	public boolean isActive(BlockPos pos, World world)
	{
		return world.getBlockState(pos).get(POWERED);
	}
	
	public void trigger(BlockPos pos, World world)
	{
		BlockState state = world.getBlockState(pos);
		state = state.with(POWERED, !state.get(POWERED));
		world.setBlockState(pos, state);
	}
}
