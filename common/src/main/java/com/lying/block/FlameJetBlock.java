package com.lying.block;

import java.util.Map;

import com.lying.block.entity.FlameJetBlockEntity;
import com.lying.init.CDBlockEntityTypes;
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

public class FlameJetBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<FlameJetBlock> CODEC = RedstoneActorBlock.createCodec(FlameJetBlock::new);
	protected static final Map<Direction, VoxelShape> COLLISION_BY_FACE = Map.of(
				Direction.UP, Block.createCuboidShape(2.5D, 0, 2.5D, 13.5D, 1, 13.5D),
				Direction.DOWN, Block.createCuboidShape(2.5D, 15, 2.5D, 13.5D, 16, 13.5D),
				Direction.NORTH, Block.createCuboidShape(2.5D, 2.5D, 15, 13.5D, 13.5D, 16),
				Direction.EAST, Block.createCuboidShape(0, 2.5D, 2.5D, 1, 13.5D, 13.5D),
				Direction.SOUTH, Block.createCuboidShape(2.5D, 2.5D, 0, 13.5D, 13.5D, 1),
				Direction.WEST, Block.createCuboidShape(15, 2.5D, 2.5D, 16, 13.5D, 13.5D)
			);
	
	protected static final VoxelShape NORTH_OUTLINE	= VoxelShapes.union(
			Block.createCuboidShape(6, 6, 15, 10, 10, 16),
			Block.createCuboidShape(9.5D, 9.5D, 15, 13.5D, 13.5D, 16),
			Block.createCuboidShape(2.5D, 9.5D, 15, 6.5D, 13.5D, 16),
			Block.createCuboidShape(9.5D, 2.5D, 15, 13.5D, 6.5D, 16),
			Block.createCuboidShape(2.5D, 2.5D, 15, 6.5D, 6.5D, 16)
			);
	protected static final VoxelShape EAST_OUTLINE	= VoxelShapes.union(
			Block.createCuboidShape(0, 6, 6, 1, 10, 10),
			Block.createCuboidShape(0, 9.5D, 9.5D, 1, 13.5D, 13.5D),
			Block.createCuboidShape(0, 9.5D, 2.5D, 1, 13.5D, 6.5D),
			Block.createCuboidShape(0, 2.5D, 9.5D, 1, 6.5D, 13.5D),
			Block.createCuboidShape(0, 2.5D, 2.5D, 1, 6.5D, 6.5D)
			);
	protected static final VoxelShape SOUTH_OUTLINE	= VoxelShapes.union(
			Block.createCuboidShape(6, 6, 0, 10, 10, 1),
			Block.createCuboidShape(2.5D, 9.5D, 0, 6.5D, 13.5D, 1),
			Block.createCuboidShape(9.5D, 9.5D, 0, 13.5D, 13.5D, 1),
			Block.createCuboidShape(2.5D, 2.5D, 0, 6.5D, 6.5D, 1),
			Block.createCuboidShape(9.5D, 2.5D, 0, 13.5D, 6.5D, 1)
			);
	protected static final VoxelShape WEST_OUTLINE	= VoxelShapes.union(
			Block.createCuboidShape(15, 6, 6, 16, 10, 10),
			Block.createCuboidShape(15, 9.5D, 9.5D, 16, 13.5D, 13.5D),
			Block.createCuboidShape(15, 9.5D, 2.5D, 16, 13.5D, 6.5D),
			Block.createCuboidShape(15, 2.5D, 9.5D, 16, 6.5D, 13.5D),
			Block.createCuboidShape(15, 2.5D, 2.5D, 16, 6.5D, 6.5D)
			);
	protected static final VoxelShape UP_OUTLINE	= VoxelShapes.union(
			Block.createCuboidShape(6, 0, 6, 10, 1, 10),
			Block.createCuboidShape(2.5D, 0, 9.5D, 6.5D, 1, 13.5D),
			Block.createCuboidShape(2.5D, 0, 2.5D, 6.5D, 1, 6.5D),
			Block.createCuboidShape(9.5D, 0, 9.5D, 13.5D, 1, 13.5D),
			Block.createCuboidShape(9.5D, 0, 2.5D, 13.5D, 1, 6.5D)
			);
	protected static final VoxelShape DOWN_OUTLINE	= VoxelShapes.union(
			Block.createCuboidShape(6, 15, 6, 10, 16, 10),
			Block.createCuboidShape(2.5D, 15, 9.5D, 6.5D, 16, 13.5D),
			Block.createCuboidShape(2.5D, 15, 2.5D, 6.5D, 16, 6.5D),
			Block.createCuboidShape(9.5D, 15, 9.5D, 13.5D, 16, 13.5D),
			Block.createCuboidShape(9.5D, 15, 2.5D, 13.5D, 16, 6.5D)
			);
	protected static final Map<Direction, VoxelShape> OUTLINE_BY_FACE = Map.of(
				Direction.UP, UP_OUTLINE,
				Direction.DOWN, DOWN_OUTLINE,
				Direction.NORTH, NORTH_OUTLINE,
				Direction.EAST, EAST_OUTLINE,
				Direction.SOUTH, SOUTH_OUTLINE,
				Direction.WEST, WEST_OUTLINE
			);
	
	public static final BooleanProperty POWERED	= Properties.POWERED;
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	
	public FlameJetBlock(Settings settingsIn)
	{
		super(settingsIn.luminance(s -> s.get(POWERED) ? 12 : 0));
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(POWERED, false));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWERED, FACING);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new FlameJetBlockEntity(pos, state);
	}
	
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return COLLISION_BY_FACE.get(state.get(FACING));
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return OUTLINE_BY_FACE.get(state.get(FACING));
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return FlameJetBlockEntity.getTicker(world, state, type);
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
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.FLAME_JET.get()).get().wireCount(); }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.FLAME_JET.get()).get().processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.FLAME_JET.get()).get().reset();
	}
}
