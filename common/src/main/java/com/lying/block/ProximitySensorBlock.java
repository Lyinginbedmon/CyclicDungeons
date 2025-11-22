package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.ProximitySensorBlockEntity;
import com.lying.init.CDBlocks;
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
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ProximitySensorBlock extends BlockWithEntity implements IWireableBlock
{
	public static final MapCodec<ProximitySensorBlock> CODEC	= createCodec(ProximitySensorBlock::new);
	private static final VoxelShape UP_SHAPE	= Block.createCuboidShape(4, 0, 4, 12, 4, 12);
	private static final VoxelShape DOWN_SHAPE	= Block.createCuboidShape(4, 12, 4, 12, 16, 12);
	private static final VoxelShape NORTH_SHAPE	= Block.createCuboidShape(4, 4, 12, 12, 12, 16);
	private static final VoxelShape EAST_SHAPE	= Block.createCuboidShape(0, 4, 4, 4, 12, 12);
	private static final VoxelShape SOUTH_SHAPE	= Block.createCuboidShape(4, 4, 0, 12, 12, 4);
	private static final VoxelShape WEST_SHAPE	= Block.createCuboidShape(12, 4, 4, 16, 12, 12);
	
	public static final IntProperty POWER	= Properties.POWER;
	public static final BooleanProperty POWERED	= Properties.POWERED;
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	
	public ProximitySensorBlock(Settings settings)
	{
		super(settings.nonOpaque().emissiveLighting(CDBlocks::always).luminance(state -> state.get(POWERED) ? 4 : 0));
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(POWER, 0).with(POWERED, false));
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, POWER, POWERED);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new ProximitySensorBlockEntity(pos, state);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		switch(state.get(FACING))
		{
			default:
			case UP:
				return UP_SHAPE;
			case DOWN:
				return DOWN_SHAPE;
			case NORTH:
				return NORTH_SHAPE;
			case SOUTH:
				return SOUTH_SHAPE;
			case EAST:
				return EAST_SHAPE;
			case WEST:
				return WEST_SHAPE;
		}
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getSide());
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return ProximitySensorBlockEntity.getTicker(world, state, type);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world) { return false; }

	@Override
	public WireRecipient type() { return WireRecipient.SENSOR; }
	
	public int activity(BlockPos pos, World world) { return world.getBlockState(pos).get(POWER); }
	
	public static void setPower(int value, BlockPos pos, World world)
	{
		BlockState state = world.getBlockState(pos);
		if(state.get(POWER) != value)
			world.setBlockState(pos, state.with(POWER, value).with(POWERED, value > 0), 3);
	}
}
