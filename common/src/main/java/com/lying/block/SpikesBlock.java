package com.lying.block;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SideShapeType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class SpikesBlock extends Block
{
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	public static final EnumProperty<SpikePart> PART	= EnumProperty.of("part", SpikePart.class);
	protected static final Map<Direction.Axis, VoxelShape> POLE_SHAPES	= Map.of(
			Direction.Axis.Y, Block.createCuboidShape(3, 0, 3, 13, 16, 13),
			Direction.Axis.Z, Block.createCuboidShape(3, 3, 0, 13, 13, 16),
			Direction.Axis.X, Block.createCuboidShape(0, 3, 3, 16, 13, 13)
			);
	protected static final Map<Direction, VoxelShape> SPIKE_SHAPES	= Map.of(
			Direction.UP, Block.createCuboidShape(3, 0, 3, 13, 14, 13),
			Direction.DOWN, Block.createCuboidShape(3, 2, 3, 13, 16, 13),
			Direction.NORTH, Block.createCuboidShape(3, 3, 2, 13, 13, 16),
			Direction.SOUTH, Block.createCuboidShape(3, 3, 0, 13, 13, 14),
			Direction.EAST, Block.createCuboidShape(0, 3, 3, 14, 13, 13),
			Direction.WEST, Block.createCuboidShape(2, 3, 3, 16, 13, 13)
			);
	
	public SpikesBlock(Settings settings)
	{
		super(settings.nonOpaque());
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(PART, SpikePart.SPIKE));
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, PART);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		Direction facing = state.get(FACING);
		switch(state.get(PART))
		{
			case POLE:	return POLE_SHAPES.get(facing.getAxis());
			case SPIKE:	return SPIKE_SHAPES.get(facing);
		}
		return VoxelShapes.empty();
	}
	
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos)
	{
		Direction facing = state.get(FACING);
		BlockPos neighbourPos = pos.offset(facing.getOpposite());
		BlockState neighbour = world.getBlockState(neighbourPos);
		return 
				(neighbour.getBlock() == state.getBlock() && neighbour.get(FACING) == facing) ||
				neighbour.isSideSolid(world, neighbourPos, facing, SideShapeType.FULL);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		BlockState state = getDefaultState().with(FACING, ctx.getSide());
		BlockState neighbour = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getSide()));
		return state.with(PART, neighbour.getBlock() == state.getBlock() ? SpikePart.POLE : SpikePart.SPIKE);
	}
	
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
		if(!canPlaceAt(state, world, pos))
			return Blocks.AIR.getDefaultState();
		
		boolean isSpike = neighborState.getBlock() == state.getBlock();
		if(direction == state.get(FACING))
		{
			switch(state.get(PART))
			{
				case SPIKE:
					// If new state is spikes, become pole
					return isSpike ? state.with(PART, SpikePart.POLE) : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
				case POLE:
					// If new state is not spikes, become spike
					return !isSpike ? state.with(PART, SpikePart.SPIKE) : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
			}
		}
		return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}
	
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity)
	{
		if(state.get(PART) == SpikePart.SPIKE && entity instanceof LivingEntity)
			skewerEntity((LivingEntity)entity, 1F);
	}
	
	public static boolean skewerEntity(LivingEntity entity, float amount)
	{
		World world = entity.getWorld();
		if(world.isClient())
			return false;
		
		DamageSource damage = entity.getWorld().getDamageSources().generic();
		return entity.damage((ServerWorld)world, damage, amount);
	}
	
	public static enum SpikePart implements StringIdentifiable
	{
		SPIKE,
		POLE;
		
		public String asString() { return name().toLowerCase(); }
	}
}
