package com.lying.block;

import java.util.Map;

import com.lying.block.entity.SpawnerActorBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SpawnerActorBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<SpawnerActorBlock> CODEC = RedstoneActorBlock.createCodec(SpawnerActorBlock::new);
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	public static final BooleanProperty POWERED	= Properties.POWERED;
	public static final Map<Direction, VoxelShape> SKULL_BY_ORIENTATION	= Map.of(
			Direction.UP, Block.createCuboidShape(3, 0, 8, 13, 2, 14),
			Direction.DOWN, Block.createCuboidShape(3, 14, 2, 13, 16, 8),
			Direction.NORTH, Block.createCuboidShape(3, 8, 14, 13, 14, 16),
			Direction.EAST, Block.createCuboidShape(0, 8, 3, 2, 14, 13),
			Direction.SOUTH, Block.createCuboidShape(3, 8, 0, 13, 14, 2),
			Direction.WEST, Block.createCuboidShape(14, 8, 3, 16, 14, 13)
			);
	public static final Map<Direction, VoxelShape> MOUTH_BY_ORIENTATION	= Map.of(
			Direction.UP, Block.createCuboidShape(4, 0, 5, 12, 1, 8),
			Direction.DOWN, Block.createCuboidShape(4, 15, 8, 12, 16, 11),
			Direction.NORTH, Block.createCuboidShape(4, 5, 15, 12, 8, 16),
			Direction.EAST, Block.createCuboidShape(0, 5, 4, 1, 8, 12),
			Direction.SOUTH, Block.createCuboidShape(4, 5, 0, 12, 8, 1),
			Direction.WEST, Block.createCuboidShape(15, 5, 4, 16, 8, 12)
			);
	public static final Map<Direction, VoxelShape> JAW_BY_ORIENTATION	= Map.of(
			Direction.UP, Block.createCuboidShape(4, 0, 2, 12, 1, 8),
			Direction.DOWN, Block.createCuboidShape(4, 15, 8, 12, 16, 14),
			Direction.NORTH, Block.createCuboidShape(4, 2, 15, 12, 8, 16),
			Direction.EAST, Block.createCuboidShape(0, 2, 4, 1, 8, 12),
			Direction.SOUTH, Block.createCuboidShape(4, 2, 0, 12, 8, 1),
			Direction.WEST, Block.createCuboidShape(15, 2, 4, 16, 8, 12)
			);
	
	public SpawnerActorBlock(Settings settingsIn)
	{
		super(settingsIn.nonOpaque());
		setDefaultState(getDefaultState().with(POWERED, false).with(FACING, Direction.NORTH));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWERED, FACING);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SpawnerActorBlockEntity(pos, state);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		Direction facing = state.get(FACING);
		VoxelShape skull = SKULL_BY_ORIENTATION.get(facing);
		return state.get(POWERED) ? 
				VoxelShapes.union(skull, JAW_BY_ORIENTATION.get(facing)) : 
				VoxelShapes.union(skull, MOUTH_BY_ORIENTATION.get(facing));
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return SpawnerActorBlockEntity.getTicker(world, state, type);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getSide());
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
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.SPAWNER.get()).get().wireCount(); }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, WireMode space, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.SPAWNER.get()).get().processWireConnection(target, space, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.SPAWNER.get()).get().reset();
	}
}
