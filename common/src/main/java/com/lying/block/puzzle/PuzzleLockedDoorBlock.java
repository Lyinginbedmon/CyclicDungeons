package com.lying.block.puzzle;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicate;
import com.lying.block.puzzle.entity.PuzzleLockedDoorBlockEntity;
import com.lying.init.CDBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class PuzzleLockedDoorBlock extends BlockWithEntity
{
	public static final MapCodec<PuzzleDoorBlock> CODEC = PuzzleDoorBlock.createCodec(PuzzleDoorBlock::new);
	public static final EnumProperty<Direction> FACING = DoorBlock.FACING;
	public static final EnumProperty<DoorHinge> HINGE = DoorBlock.HINGE;
	public static final EnumProperty<DoubleBlockHalf> HALF = DoorBlock.HALF;
	public static final BooleanProperty LOCKED	= BooleanProperty.of("locked");
	public static final BooleanProperty OPEN	= Properties.OPEN;
	protected static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
	protected static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);
	protected static final VoxelShape EAST_SHAPE = Block.createCuboidShape(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	protected static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);
	private static final Predicate<BlockState> isDoor = s -> s.getBlock() instanceof DoorBlock || s.isOf(CDBlocks.PUZZLE_LOCKED_DOOR.get());
	
	public PuzzleLockedDoorBlock(Settings settingsIn)
	{
		super(settingsIn.nonOpaque());
		setDefaultState(getDefaultState()
			.with(FACING, Direction.NORTH)
			.with(HINGE, DoorHinge.LEFT)
			.with(HALF, DoubleBlockHalf.LOWER)
			.with(OPEN, false)
			.with(LOCKED, true));
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return state.get(HALF) == DoubleBlockHalf.LOWER ? new PuzzleLockedDoorBlockEntity(pos, state) : null;
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return PuzzleLockedDoorBlockEntity.getTicker(world, state, type);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		Direction direction = state.get(FACING);
		boolean isOpen = !(Boolean)state.get(OPEN);
		boolean isRightHinge = state.get(HINGE) == DoorHinge.RIGHT;
		
		return switch (direction)
				{
					case SOUTH -> isOpen ? NORTH_SHAPE : (isRightHinge ? WEST_SHAPE : EAST_SHAPE);
					case WEST -> isOpen ? EAST_SHAPE : (isRightHinge ? NORTH_SHAPE : SOUTH_SHAPE);
					case NORTH -> isOpen ? SOUTH_SHAPE : (isRightHinge ? EAST_SHAPE : WEST_SHAPE);
					default -> isOpen ? WEST_SHAPE : (isRightHinge ? SOUTH_SHAPE : NORTH_SHAPE);
				};
	}
	
	@Override
	protected void appendProperties(Builder<Block, BlockState> builder)
	{
		builder.add(HALF, FACING, HINGE, LOCKED, OPEN);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockPos blockPos = ctx.getBlockPos();
		World world = ctx.getWorld();
		if(blockPos.getY() < world.getTopYInclusive() && world.getBlockState(blockPos.up()).canReplace(ctx))
		{
			return this.getDefaultState()
				.with(FACING, ctx.getHorizontalPlayerFacing())
				.with(HINGE, this.getHinge(ctx))
				.with(HALF, DoubleBlockHalf.LOWER);
		}
		else
			return null;
	}
	
	private DoorHinge getHinge(ItemPlacementContext ctx)
	{
		
		BlockView blockView = ctx.getWorld();
		BlockPos blockPos = ctx.getBlockPos();
		Direction look = ctx.getHorizontalPlayerFacing();
		BlockPos posUp = blockPos.up();
		Direction lookLeft = look.rotateYCounterclockwise();
		BlockPos posLeft = blockPos.offset(lookLeft);
		BlockState stateLeft = blockView.getBlockState(posLeft);
		BlockPos blockPos4 = posUp.offset(lookLeft);
		BlockState blockState2 = blockView.getBlockState(blockPos4);
		Direction lookRight = look.rotateYClockwise();
		BlockPos posRight = blockPos.offset(lookRight);
		BlockState stateRight = blockView.getBlockState(posRight);
		BlockPos blockPos6 = posUp.offset(lookRight);
		BlockState blockState4 = blockView.getBlockState(blockPos6);
		int i = (stateLeft.isFullCube(blockView, posLeft) ? -1 : 0)
			+ (blockState2.isFullCube(blockView, blockPos4) ? -1 : 0)
			+ (stateRight.isFullCube(blockView, posRight) ? 1 : 0)
			+ (blockState4.isFullCube(blockView, blockPos6) ? 1 : 0);
		boolean doorAtLeft = isDoor.test(stateLeft) && stateLeft.get(HALF) == DoubleBlockHalf.LOWER;
		boolean doorAtRight = isDoor.test(stateRight) && stateRight.get(HALF) == DoubleBlockHalf.LOWER;
		if ((!doorAtLeft || doorAtRight) && i <= 0)
		{
			if ((!doorAtRight || doorAtLeft) && i >= 0)
			{
				int j = look.getOffsetX();
				int k = look.getOffsetZ();
				Vec3d vec3d = ctx.getHitPos();
				double d = vec3d.x - (double)blockPos.getX();
				double e = vec3d.z - (double)blockPos.getZ();
				return (j >= 0 || !(e < 0.5)) && (j <= 0 || !(e > 0.5)) && (k >= 0 || !(d > 0.5)) && (k <= 0 || !(d < 0.5)) ? DoorHinge.LEFT : DoorHinge.RIGHT;
			}
			else
				return DoorHinge.LEFT;
		}
		else
			return DoorHinge.RIGHT;
	}
	
	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack)
	{
		world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
	}
	
	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && (player.isCreative() || !player.canHarvest(state)))
		{
			world.breakBlock(state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos.up(), false);
		}
		
		return super.onBreak(world, pos, state, player);
	}
	
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit)
	{
		if(state.get(LOCKED))
			return ActionResult.PASS;
		
		state = state.cycle(OPEN);
		world.setBlockState(pos, state, 10);
		this.playOpenCloseSound(player, world, pos, state.get(OPEN));
		world.emitGameEvent(player, state.get(OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		return ActionResult.SUCCESS;
	}
	
	private void playOpenCloseSound(@Nullable Entity entity, World world, BlockPos pos, boolean open)
	{
		world.playSound(
			entity, pos, open ? BlockSetType.IRON.doorOpen() : BlockSetType.IRON.doorClose(), SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F
		);
	}
	
	protected BlockState rotate(BlockState state, BlockRotation rotation)
	{
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}
	
	protected BlockState mirror(BlockState state, BlockMirror mirror)
	{
		return mirror == BlockMirror.NONE ? state : state.rotate(mirror.getRotation(state.get(FACING))).cycle(HINGE);
	}
}