package com.lying.block;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class CrumblingBlockResetting extends CrumblingBlock
{
	public static final IntProperty BREAK	= Properties.AGE_3;
	
	public CrumblingBlockResetting(Settings settings, Supplier<Block> emulatedIn)
	{
		super(settings.ticksRandomly(), emulatedIn);
		setDefaultState(getDefaultState().with(BREAK, 0));
	}
	
	protected boolean hasRandomTicks(BlockState state) { return state.get(BREAK) > 0; }
	
	protected BlockRenderType getRenderType(BlockState state)
	{
		return isBroken(state) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
	}
	
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return isBroken(state) ? VoxelShapes.empty() : VoxelShapes.fullCube();
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView World, BlockPos pos, ShapeContext context)
	{
		return isBroken(state) ? VoxelShapes.empty() : VoxelShapes.fullCube();
	}
	
	protected boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos)
	{
		return isBroken(state) ? false : super.isShapeFullCube(state, world, pos);
	}
	
	public static boolean isBroken(BlockState state)
	{
		return state.get(BREAK) > 0;
	}
	
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random)
	{
		if(isBroken(state) && state.canPlaceAt(world, pos))
			world.setBlockState(pos, state.with(BREAK, state.get(BREAK) - 1));
	}
	
	protected void crumble(World world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		world.breakBlock(pos, false);
		world.setBlockState(pos, state.with(BREAK, 3));
	}
	
	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
		)
	{
		return !this.canPlaceAt(state, world, pos)
			? this.getDefaultState().with(BREAK, 3)
			: super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(BREAK);
	}
}
