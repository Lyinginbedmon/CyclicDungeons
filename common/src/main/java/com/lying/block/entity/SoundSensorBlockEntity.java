package com.lying.block.entity;

import org.jetbrains.annotations.Nullable;

import com.lying.block.IWireableBlock;
import com.lying.block.SoundSensorBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.entity.Entity;
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
	
	public SoundSensorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SOUND_SENSOR.get(), pos, state);
		listener = new VibrationListener(this);
		listenerData = new ListenerData();
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
				IWireableBlock.getWireable(this.pos, world).trigger(this.pos, world);
		}
		
		public void onListen()
		{
			SoundSensorBlockEntity.this.markDirty();
		}
		
		public boolean requiresTickingChunksAround() { return true; }
	}
}
