package com.lying.block;

import com.lying.init.CDBlockEntityTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
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

public class HatchBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<HatchBlock> CODEC = RedstoneActorBlock.createCodec(HatchBlock::new);

	public static final BooleanProperty POWERED	= Properties.POWERED;
	public static final BooleanProperty INTERSTITIAL = BooleanProperty.of("interstitial");
	public static final EnumProperty<Direction> FACING	= Properties.HORIZONTAL_FACING;
	
	public static final VoxelShape SHAPE_CLOSED	= Block.createCuboidShape(0, 14, 0, 16, 16, 16);
	public static final VoxelShape SHAPE_OPEN	= VoxelShapes.empty();
	
	public HatchBlock(Settings settingsIn)
	{
		super(settingsIn);
		setDefaultState(getDefaultState().with(POWERED, false).with(FACING, Direction.NORTH).with(INTERSTITIAL, false));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec()
	{
		return CODEC;
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWERED, FACING, INTERSTITIAL);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState()
				.with(FACING, ctx.getHorizontalPlayerFacing())
				.with(INTERSTITIAL, isInterstitial(ctx.getHorizontalPlayerFacing(), ctx.getWorld(), ctx.getBlockPos()));
	}
	
	public BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random)
	{
		return state.with(INTERSTITIAL, isInterstitial(state.get(FACING), world, pos));
	}
	
	protected static boolean isInterstitial(Direction face, WorldView world, BlockPos pos)
	{
		return world.getBlockState(pos.offset(face.getOpposite())).getBlock() instanceof HatchBlock;
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return SHAPE_CLOSED;
	}
	
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return state.get(POWERED) ? SHAPE_OPEN : SHAPE_CLOSED;
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
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().wireCount(); }
}
