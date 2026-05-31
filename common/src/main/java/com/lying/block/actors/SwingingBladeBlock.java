package com.lying.block.actors;

import org.jetbrains.annotations.Nullable;

import com.lying.block.Port;
import com.lying.block.actors.entity.SwingingBladeBlockEntity;
import com.lying.block.entity.logic.PortEntry;
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
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SwingingBladeBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<SwingingBladeBlock> CODEC = RedstoneActorBlock.createCodec(SwingingBladeBlock::new);
	protected static final VoxelShape NORTH_SHAPE	= Block.createCuboidShape(3, 3, 8, 13, 13, 16);
	protected static final VoxelShape EAST_SHAPE	= Block.createCuboidShape(0, 3, 3, 8, 13, 13);
	protected static final VoxelShape SOUTH_SHAPE	= Block.createCuboidShape(3, 3, 0, 13, 13, 8);
	protected static final VoxelShape WEST_SHAPE	= Block.createCuboidShape(8, 3, 3, 16, 13, 13);
	protected static final VoxelShape UP_SHAPE		= Block.createCuboidShape(3, 0, 3, 13, 8, 13);
	protected static final VoxelShape DOWN_SHAPE	= Block.createCuboidShape(3, 8, 3, 13, 16, 13);
	
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	public static final EnumProperty<Direction.Axis> AXIS	= Properties.AXIS;
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
	public SwingingBladeBlock(Settings settings)
	{
		super(settings.nonOpaque());
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(AXIS, Axis.X).with(POWERED, false));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec()
	{
		return CODEC;
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, AXIS, POWERED);
	}
	
	public Vec3d wireRenderOffset(BlockState state)
	{
		Direction face = state.get(FACING);
		return new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ()).multiply(0.3).negate();
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SwingingBladeBlockEntity(pos, state);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		switch(state.get(FACING))
		{
			case NORTH:	return NORTH_SHAPE;
			case EAST:	return EAST_SHAPE;
			case SOUTH:	return SOUTH_SHAPE;
			case WEST:	return WEST_SHAPE;
			case DOWN:	return DOWN_SHAPE;
			case UP:
			default:	return UP_SHAPE;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return SwingingBladeBlockEntity.getTicker(world, state, type);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		Direction.Axis look = ctx.getPlayerLookDirection().getAxis();
		Direction face = ctx.getSide();
		if(face.getAxis() == look)
			switch(look)
			{
				case X:
					look = Direction.Axis.Z;
					break;
				case Y:
				case Z:
					look = Direction.Axis.X;
					break;
			}
		
		return getDefaultState()
				.with(FACING, face)
				.with(AXIS, look);
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.SWINGING_BLADE.get()).get().wireCount(); }
	
	public boolean acceptWireFrom(Port input, BlockPos target, WireMode space, PortEntry output, World world)
	{
		world.getBlockEntity(target, CDBlockEntityTypes.SWINGING_BLADE.get()).ifPresent(t -> t.processInputConnection(input, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.SWINGING_BLADE.get()).get().reset();
	}
	
	public void trigger(BlockPos pos, World world)
	{
		BlockState state = world.getBlockState(pos);
		world.setBlockState(pos, state.with(POWERED, !state.get(POWERED)), 3);
	}
	
	public boolean isActive(BlockPos pos, World world) { return world.getBlockState(pos).get(POWERED); }
}
