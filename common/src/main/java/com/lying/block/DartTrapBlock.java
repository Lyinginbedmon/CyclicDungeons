package com.lying.block;

import java.util.Map;

import com.lying.block.entity.DartTrapBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class DartTrapBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<FlameJetBlock> CODEC = RedstoneActorBlock.createCodec(FlameJetBlock::new);
	public static final BooleanProperty POWERED	= Properties.POWERED;
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	protected static final Map<Direction, VoxelShape> OUTLINE_BY_FACE = Map.of(
				Direction.UP, Block.createCuboidShape(6, 0, 6, 10, 1, 10),
				Direction.DOWN, Block.createCuboidShape(6, 15, 6, 10, 16, 10),
				Direction.NORTH, Block.createCuboidShape(6, 6, 15, 10, 10, 16),
				Direction.WEST, Block.createCuboidShape(15, 6, 6, 16, 10, 10),
				Direction.SOUTH, Block.createCuboidShape(6, 6, 0, 10, 10, 1),
				Direction.EAST, Block.createCuboidShape(0, 6, 6, 1, 10, 10)
			);
	
	protected static final float DART_POWER = 11F;
	protected static final float DART_ERROR = 0.6F;
	
	public DartTrapBlock(Settings settingsIn)
	{
		super(settingsIn.noCollision());
		setDefaultState(getDefaultState().with(POWERED, false).with(FACING, Direction.UP));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, POWERED);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new DartTrapBlockEntity(pos, state);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return OUTLINE_BY_FACE.get(state.get(FACING));
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getSide());
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return DartTrapBlockEntity.getTicker(world, state, type);
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.DART_TRAP.get()).get().wireCount(); }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.DART_TRAP.get()).get().processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.DART_TRAP.get()).get().reset();
	}
	
	public boolean isActive(BlockPos pos, World world)
	{
		return world.getBlockState(pos).get(POWERED);
	}
	
	public void trigger(BlockPos pos, World world)
	{
		BlockState state = world.getBlockState(pos);
		boolean isActive = !isActive(pos, world);
		
		world.setBlockState(pos, state.with(POWERED, isActive));
		if(isActive && !world.isClient())
			shootDart(pos, (ServerWorld)world);
		
	}
	
	protected void shootDart(BlockPos pos, ServerWorld world)
	{
		world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS);
		BlockState state = world.getBlockState(pos);
		Direction direction = state.get(FACING);
		DartTrapBlockEntity tileEntity = world.getBlockEntity(pos, CDBlockEntityTypes.DART_TRAP.get()).get();
		ProjectileEntity.spawnWithVelocity(
				tileEntity.createDart(world, getDartOrigin(pos, direction.getOpposite()), direction),
				world,
				new ItemStack(Items.ARROW),
				direction.getOffsetX(),
				direction.getOffsetY(),
				direction.getOffsetZ(),
				DART_POWER,
				DART_ERROR
				);
	}
	
	protected Position getDartOrigin(BlockPos pos, Direction direction)
	{
		return new Vec3d(pos.getX(), pos.getY(), pos.getZ())
				.add(0.5D)
				.add(
					direction.getOffsetX() * 0.5D, 
					direction.getOffsetY() * 0.5D, 
					direction.getOffsetZ() * 0.5D);
	}
}
