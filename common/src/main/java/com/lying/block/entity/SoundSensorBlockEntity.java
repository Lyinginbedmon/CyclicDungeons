package com.lying.block.entity;

import org.jetbrains.annotations.Nullable;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.SoundSensorBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.Vibrations.VibrationListener;
import net.minecraft.world.event.listener.GameEventListener.Holder;

public class SoundSensorBlockEntity extends BlockEntity implements Holder<VibrationListener>, Vibrations
{
	private final VibrationListener listener;
	private ListenerData listenerData;
	private int lastFrequency;
	
	public SoundSensorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SOUND_SENSOR.get(), pos, state);
		listener = new VibrationListener(this);
		listenerData = new ListenerData();
	}
	
	protected void writeNbt(NbtCompound nbt, WrapperLookup registries)
	{
		super.writeNbt(nbt, registries);
		nbt.putInt("last_vibration_frequency", lastFrequency);
		RegistryOps<NbtElement> registryOps = registries.getOps(NbtOps.INSTANCE);
		ListenerData.CODEC
			.encodeStart(registryOps, listenerData)
			.resultOrPartial(string -> CyclicDungeons.LOGGER.error("Failed to encode vibration listener for Sound Sensor: '{}'", string))
			.ifPresent(listenerNbt -> nbt.put("listener", listenerNbt));
	}
	
	protected void readNbt(NbtCompound nbt, WrapperLookup registries)
	{
		super.readNbt(nbt, registries);
		this.lastFrequency = nbt.getInt("last_vibration_frequency");
		RegistryOps<NbtElement> registryOps = registries.getOps(NbtOps.INSTANCE);
		if(nbt.contains("listener", 10))
			ListenerData.CODEC
				.parse(registryOps, nbt.getCompound("listener"))
				.resultOrPartial(string -> CyclicDungeons.LOGGER.error("Failed to parse vibration listener for Sound Sensor: '{}'", string))
				.ifPresent(listener -> this.listenerData = listener);
	}
	
	public VibrationListener getEventListener()
	{
		return listener;
	}
	
	public ListenerData getVibrationListenerData()
	{
		return listenerData;
	}
	
	public Callback getVibrationCallback()
	{
		return new SoundSensorBlockEntity.VibrationCallback(getPos());
	}
	
	protected class VibrationCallback implements Callback
	{
		protected final BlockPos pos;
		private final PositionSource positionSource;
		
		public VibrationCallback(final BlockPos pos)
		{
			this.pos = pos;
			this.positionSource = new BlockPositionSource(pos);
		}
		
		public int getRange() { return 8; }
		
		public PositionSource getPositionSource() { return this.positionSource; }
		
		public boolean triggersAvoidCriterion() { return true; }
		
		@SuppressWarnings("deprecation")
		@Override
		public boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, @Nullable Emitter emitter)
		{
			return !pos.equals(this.pos) || !event.matches(GameEvent.BLOCK_DESTROY) && !event.matches(GameEvent.BLOCK_PLACE)
				? getCachedState().get(SoundSensorBlock.PHASE) == SculkSensorPhase.INACTIVE
				: false;
		}
		
		public void accept(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
			BlockState blockState = SoundSensorBlockEntity.this.getCachedState();
			if(blockState.getBlock() == CDBlocks.SENSOR_SOUND.get() && blockState.get(SoundSensorBlock.PHASE) == SculkSensorPhase.INACTIVE)
			{
				SoundSensorBlockEntity.this.lastFrequency = Vibrations.getFrequency(event);
				int i = Vibrations.getSignalStrength(distance, getRange());
				IWireableBlock.getWireable(this.pos, world).trigger(this.pos, world);
				world.setBlockState(this.pos, world.getBlockState(this.pos).with(SoundSensorBlock.POWER, i));
			}
		}
		
		public void onListen()
		{
			SoundSensorBlockEntity.this.markDirty();
		}
		
		public boolean requiresTickingChunksAround() { return true; }
	}
}
