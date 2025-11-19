package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.SoundSensorBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.Vibrations;

public class SoundSensorBlock extends BlockWithEntity implements IWireableBlock
{
	public static final MapCodec<SoundSensorBlock> CODEC	= createCodec(SoundSensorBlock::new);
	
	public static final EnumProperty<SculkSensorPhase> PHASE	= Properties.SCULK_SENSOR_PHASE;
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	
	public SoundSensorBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(PHASE, SculkSensorPhase.INACTIVE));
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SoundSensorBlockEntity(pos, state);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec()
	{
		return CODEC;
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, PHASE);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return !world.isClient
				? validateTicker(
					type,
					CDBlockEntityTypes.SOUND_SENSOR.get(),
					(worldx, pos, statex, blockEntity) -> Vibrations.Ticker.tick(worldx, blockEntity.getVibrationListenerData(), blockEntity.getVibrationCallback())
				)
				: null;
	}
	
	public WireRecipient type() { return WireRecipient.SENSOR; }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return false;
	}
	
	public boolean isActive(BlockPos pos, World world)
	{
		return world.getBlockState(pos).get(PHASE) == SculkSensorPhase.ACTIVE;
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getSide());
	}
	
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random)
	{
		switch(state.get(PHASE))
		{
			case COOLDOWN:
				world.setBlockState(pos, state.with(PHASE, SculkSensorPhase.INACTIVE), 3);
				world.playSound(null, pos, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING_STOP, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.8F);
				break;
			default:
				world.setBlockState(pos, state.with(PHASE, SculkSensorPhase.COOLDOWN), 3);
				world.scheduleBlockTick(pos, state.getBlock(), 10);
				break;
		}
	}
	
	public void trigger(BlockPos pos, World world)
	{
		BlockState state = world.getBlockState(pos);
		world.setBlockState(pos, state.with(PHASE, SculkSensorPhase.ACTIVE));
		world.scheduleBlockTick(pos, state.getBlock(), 30);
		world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.BLOCKS, 1F, world.random.nextFloat() * 0.2F + 0.8F);
	}
}
