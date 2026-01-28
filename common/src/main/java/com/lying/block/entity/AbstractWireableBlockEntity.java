package com.lying.block.entity;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractWireableBlockEntity extends BlockEntity
{
	private static final Function<int[], BlockPos> intArrayToBlockPos = v -> new BlockPos(v[0], v[1], v[2]);
	private List<BlockPos> actors = Lists.newArrayList();
	private List<BlockPos> sensors = Lists.newArrayList();
	
	protected AbstractWireableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	// FIXME Permit wiring to be done via NBT loading
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		// Blank NBT isn't read when the block entity updates, so we ensure there's always SOME data
		nbt.putBoolean("IsActive", IWireableBlock.getWireable(getPos(), getWorld()).isActive(getPos(), getWorld()));
		
		if(hasSensors())
		{
			NbtList set = new NbtList();
			sensors.forEach(p -> set.add(NbtHelper.fromBlockPos(p)));
			nbt.put("Sensors", set);
		}
		
		if(hasActors())
		{
			NbtList set = new NbtList();
			actors.forEach(p -> set.add(NbtHelper.fromBlockPos(p)));
			nbt.put("Actors", set);
		}
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		sensors.clear();
		if(nbt.contains("Sensors"))
		{
			NbtList set = nbt.getList("Sensors", NbtElement.INT_ARRAY_TYPE);
			for(int i=0; i<set.size(); i++)
				addWire(intArrayToBlockPos.apply(set.getIntArray(i)), WireRecipient.SENSOR);
		}
		
		actors.clear();
		if(nbt.contains("Actors"))
		{
			NbtList set = nbt.getList("Actors", NbtElement.INT_ARRAY_TYPE);
			for(int i=0; i<set.size(); i++)
				addWire(intArrayToBlockPos.apply(set.getIntArray(i)), WireRecipient.ACTOR);
		}
	}
	
	public final List<BlockPos> getSensors() { return sensors; }
	public final List<BlockPos> getActors() { return actors; }
	
	public final boolean hasSensors() { return !sensors.isEmpty(); }
	public final boolean hasActors() { return !actors.isEmpty(); }
	
	public final int wireCount() { return sensors.size() + actors.size(); }
	
	public abstract boolean processWireConnection(BlockPos pos, WireRecipient type);
	
	public void reset()
	{
		// Deactivate any attached actors
		actors.forEach(p -> IWireableBlock.getWireable(p, world).deactivate(p, world));
		
		// Clear connections
		actors.clear();
		sensors.clear();
		
		// Deactivate self
		IWireableBlock wireable = IWireableBlock.getWireable(getPos(), getWorld());
		if(wireable.isActive(getPos(), getWorld()))
			wireable.deactivate(getPos(), getWorld());
		
		markDirty();
	}
	
	protected void cleanActors()
	{
		if(actors.removeIf(pos -> 
		{
			BlockState sensorState = world.getBlockState(pos);
			return !(sensorState.getBlock() instanceof IWireableBlock) || IWireableBlock.getWireable(pos, world).type() != WireRecipient.ACTOR;
		}))
			markDirty();
	}
	
	protected void cleanSensors()
	{
		if(sensors.removeIf(pos -> 
		{
			BlockState sensorState = world.getBlockState(pos);
			return !(sensorState.getBlock() instanceof IWireableBlock) || IWireableBlock.getWireable(pos, world).type() != WireRecipient.SENSOR;
		}))
			markDirty();
	}
	
	protected final void addWire(BlockPos pos, WireRecipient type)
	{
		switch(type)
		{
			case ACTOR:
				if(actors.stream().anyMatch(p -> p.getManhattanDistance(pos) == 0))
					return;
				actors.add(pos);
				markDirty();
				break;
			case SENSOR:
				if(sensors.stream().anyMatch(p -> p.getManhattanDistance(pos) == 0))
					return;
				sensors.add(pos);
				markDirty();
				break;
			case LOGIC:
			default:
				break;
		}
	}
	
	public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
	
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registries) { return createNbt(registries); }
	
	public void markDirty()
	{
		if(world != null)
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}
}
