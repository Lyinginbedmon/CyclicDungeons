package com.lying.block;

import com.lying.block.entity.SpawnerActorBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
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
import net.minecraft.world.World;

public class SpawnerActorBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<SpawnerActorBlock> CODEC = RedstoneActorBlock.createCodec(SpawnerActorBlock::new);
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
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
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.SPAWNER.get()).get().processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.SPAWNER.get()).get().reset();
	}
}
