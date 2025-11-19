package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.SightSensorBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SightSensorBlock extends BlockWithEntity implements IWireableBlock
{
	public static final MapCodec<SightSensorBlock> CODEC	= createCodec(SightSensorBlock::new);
	
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
	public SightSensorBlock(Settings settings)
	{
		super(settings.nonOpaque());
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SightSensorBlockEntity(pos, state);
	}
	
	protected BlockRenderType getRenderType(BlockState state)
	{
		return BlockRenderType.INVISIBLE;
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWERED);
	}
	
	public boolean isActive(BlockPos pos, World world) { return world.getBlockState(pos).get(POWERED); }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return false;
	}
	
	public WireRecipient type() { return WireRecipient.SENSOR; }
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return SightSensorBlockEntity.getTicker(world, state, type);
	}
}
