package com.lying.block;

import java.util.Map;
import java.util.Optional;

import com.lying.block.entity.SpikeTrapBlockEntity;
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

public class SpikeTrapBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<SpikeTrapBlock> CODEC = RedstoneActorBlock.createCodec(SpikeTrapBlock::new);
	public static final BooleanProperty POWERED	= Properties.POWERED;
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	protected static final Map<Direction, VoxelShape> BASE_SHAPE = Map.of(
			Direction.UP, Block.createCuboidShape(1, 0, 1, 15, 1, 15),
			Direction.DOWN, Block.createCuboidShape(1, 15, 1, 15, 16, 15),
			Direction.NORTH, Block.createCuboidShape(1, 1, 15, 15, 15, 16),
			Direction.WEST, Block.createCuboidShape(15, 1, 1, 16, 15, 15),
			Direction.SOUTH, Block.createCuboidShape(1, 1, 0, 15, 15, 1),
			Direction.EAST, Block.createCuboidShape(0, 1, 1, 1, 15, 15)
		);
	
	public SpikeTrapBlock(Settings settingsIn)
	{
		super(settingsIn.dynamicBounds());
		setDefaultState(getDefaultState().with(POWERED, false).with(FACING, Direction.UP));
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, POWERED);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SpikeTrapBlockEntity(pos, state);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getSide());
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return SpikeTrapBlockEntity.getTicker(world, state, type);
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.SPIKE_TRAP.get()).get().wireCount(); }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.SPIKE_TRAP.get()).get().processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.SPIKE_TRAP.get()).get().reset();
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
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		VoxelShape shape = getBaseShape(state.get(FACING));
		if(!CDBlockEntityTypes.SPIKE_TRAP.isPresent())
			return shape;
		
		Optional<SpikeTrapBlockEntity> opt;
		return (opt = world.getBlockEntity(pos, CDBlockEntityTypes.SPIKE_TRAP.get())).isPresent() && opt.get().extension(1F) > 0F && !opt.get().getExtensionShape().isEmpty()
				? VoxelShapes.union(shape, opt.get().getExtensionShape())
				: shape;
	}
	
	public static VoxelShape getBaseShape(Direction facing)
	{
		return BASE_SHAPE.get(facing);
	}
}
