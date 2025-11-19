package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.reference.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class CollisionSensorBlock extends AbstractTrapSensorBlock
{
	protected static final Box UP_BOUNDS	= new Box(0.0625, 0, 0.0625, 0.9375, 0.0625, 0.9375);
	protected static final Box DOWN_BOUNDS	= new Box(0.0625, 0.9375, 0.0625, 0.9375, 1, 0.9375);
	protected static final Box NORTH_BOUNDS	= new Box(0.0625, 0.0625, 0.875, 0.9375, 0.9375, 1);
	protected static final Box EAST_BOUNDS	= new Box(0, 0.0625, 0.0625, 0.125, 0.9375, 0.9375);
	protected static final Box SOUTH_BOUNDS	= new Box(0.0625, 0.0625, 0, 0.9375, 0.9375, 0.125);
	protected static final Box WEST_BOUNDS	= new Box(0.875, 0.0625, 0.0625, 1, 0.9375, 0.9375);
	
	protected static final VoxelShape UP_SHAPE		= Block.createCuboidShape(1, 0, 1, 15, 2, 15);
	protected static final VoxelShape DOWN_SHAPE	= Block.createCuboidShape(1, 14, 1, 15, 16, 15);
	protected static final VoxelShape NORTH_SHAPE	= Block.createCuboidShape(1, 1, 14, 15, 15, 16);
	protected static final VoxelShape EAST_SHAPE	= Block.createCuboidShape(0, 1, 1, 2, 15, 15);
	protected static final VoxelShape SOUTH_SHAPE	= Block.createCuboidShape(1, 1, 0, 15, 15, 2);
	protected static final VoxelShape WEST_SHAPE	= Block.createCuboidShape(14, 1, 1, 16, 15, 15);
	
	protected static final VoxelShape UP_SHAPE_PRESSED		= Block.createCuboidShape(1, 0, 1, 15, 1, 15);
	protected static final VoxelShape DOWN_SHAPE_PRESSED	= Block.createCuboidShape(1, 15, 1, 15, 16, 15);
	protected static final VoxelShape NORTH_SHAPE_PRESSED	= Block.createCuboidShape(1, 1, 15, 15, 15, 16);
	protected static final VoxelShape EAST_SHAPE_PRESSED	= Block.createCuboidShape(0, 1, 1, 1, 15, 15);
	protected static final VoxelShape SOUTH_SHAPE_PRESSED	= Block.createCuboidShape(1, 1, 0, 15, 15, 1);
	protected static final VoxelShape WEST_SHAPE_PRESSED	= Block.createCuboidShape(15, 1, 1, 16, 15, 15);
	
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	
	public CollisionSensorBlock(Settings settings)
	{
		super(settings
				.solid()
				.noCollision());
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(POWERED, false));
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, POWERED);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		boolean pressed = state.get(POWERED);
		switch(state.get(FACING))
		{
			case UP:	return pressed ? UP_SHAPE_PRESSED : UP_SHAPE;
			case DOWN:	return pressed ? DOWN_SHAPE_PRESSED : DOWN_SHAPE;
			case NORTH:	return pressed ? NORTH_SHAPE_PRESSED : NORTH_SHAPE;
			case EAST:	return pressed ? EAST_SHAPE_PRESSED : EAST_SHAPE;
			case SOUTH:	return pressed ? SOUTH_SHAPE_PRESSED : SOUTH_SHAPE;
			case WEST:	return pressed ? WEST_SHAPE_PRESSED : WEST_SHAPE;
		}
		return UP_SHAPE;
	}
	
	protected static Box getSearchBounds(Direction face)
	{
		switch(face)
		{
			default:
			case UP:	return UP_BOUNDS;
			case DOWN:	return DOWN_BOUNDS;
			case NORTH:	return NORTH_BOUNDS;
			case EAST:	return EAST_BOUNDS;
			case SOUTH:	return SOUTH_BOUNDS;
			case WEST:	return WEST_BOUNDS;
		}
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getSide());
	}
	
	public boolean isActive(BlockPos pos, World world)
	{
		return world.getBlockState(pos).get(POWERED);
	}
	
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random)
	{
		updatePowered(null, world, pos, state, shouldBePowered(world, pos, state));
	}
	
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity)
	{
		if(!world.isClient())
			updatePowered(entity, world, pos, state, shouldBePowered(world, pos, state));
	}
	
	private void updatePowered(@Nullable Entity entity, World world, BlockPos pos, BlockState state, boolean power)
	{
		boolean powerState = state.get(POWERED);
		if(powerState != power)
		{
			BlockState newState = state.with(POWERED, power);
			world.setBlockState(pos, newState, 2);
			world.scheduleBlockRerenderIfNeeded(pos, state, newState);
		}
		
		// Falling edge trigger
		if(!power && powerState)
		{
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF, SoundCategory.BLOCKS);
			world.emitGameEvent(entity, GameEvent.BLOCK_DEACTIVATE, pos);
		}
		// Rising edge trigger
		else if(power && !powerState)
		{
			world.playSound(null, pos, SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, SoundCategory.BLOCKS);
			world.emitGameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
		}
		
		if(power)
			world.scheduleBlockTick(pos, this, Reference.Values.TICKS_PER_SECOND);
	}
	
	protected boolean shouldBePowered(World world, BlockPos pos, BlockState state)
	{
		Box bounds = getSearchBounds(state.get(FACING)).offset(pos);
		
		// Any creatures
		if(world.getEntitiesByClass(LivingEntity.class, bounds, EntityPredicates.EXCEPT_SPECTATOR.and(entity -> !entity.canAvoidTraps())).size() > 0)
			return true;
		
		// Any sufficiently-heavy items
		if(world.getEntitiesByClass(ItemEntity.class, bounds, e -> 
		{
			ItemEntity entity = (ItemEntity)e;
			ItemStack stack = entity.getStack();
			return stack.getMaxCount() <= 16;
		}).size() > 0)
			return true;
		
		return false;
	}
}
