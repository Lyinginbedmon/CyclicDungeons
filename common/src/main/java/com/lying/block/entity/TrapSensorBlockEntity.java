package com.lying.block.entity;

import java.util.List;

import com.lying.item.WiringGunItem.WireMode;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class TrapSensorBlockEntity<T extends TrapSensorBlockEntity<?>> extends AbstractWireableBlockEntity
{
	protected int minTicksActive = 0;
	protected int activeTicks = 0;
	
	protected TrapSensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		NbtCompound settings = new NbtCompound();
		storeSettings(settings);
		nbt.put("Settings", settings);
		
		if(activeTicks > 0)
			nbt.putInt("Activity", activeTicks);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		loadSettings(nbt.getCompound("Settings"));
		activeTicks = nbt.getInt("Activity");
	}
	
	public List<String> inputPorts() { return List.of(); }
	
	protected void storeSettings(NbtCompound nbt)
	{
		if(minTicksActive > 0)
			nbt.putInt("Delay", minTicksActive);
	}
	
	protected void loadSettings(NbtCompound nbt)
	{
		minTicksActive = nbt.getInt("Delay");
	}
	
	public boolean processInputConnection(String input, BlockPos pos, String port, WireMode space)
	{
		return true;
	}
	
	public boolean processOutputConnection(String output, BlockPos pos, String input, WireMode space)
	{
		return false;
	}
	
	public abstract boolean shouldBeActive(T tile);
	
	public abstract void runActive(T tile);
	
	public abstract void runInactive(T tile);
	
	public void setActivationDelay(int delay) { this.minTicksActive = delay; }
	
	public static <T extends TrapSensorBlockEntity<T>> void tickClient(World world, BlockPos pos, BlockState state, T tile) { }
	
	public static <T extends TrapSensorBlockEntity<T>> void tickServer(World world, BlockPos pos, BlockState state, T tile)
	{
		if(tile.shouldBeActive(tile))
		{
			if(tile.activeTicks++ > tile.minTicksActive)
				tile.runActive(tile);
			tile.markDirty();
		}
		else if(tile.activeTicks > 0)
		{
			tile.activeTicks = 0;
			tile.runInactive(tile);
			tile.markDirty();
		}
	}
	
	public void resetBlock() { }
}
