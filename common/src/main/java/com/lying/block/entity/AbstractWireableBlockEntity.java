package com.lying.block.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.PortEntry;
import com.lying.block.entity.logic.WiringManifest;
import com.lying.block.entity.logic.WiringManifest.ManifestEntry;
import com.lying.item.WiringGunItem.WireMode;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractWireableBlockEntity extends BlockEntity
{
	protected WiringManifest wiringGlobal = new WiringManifest();
	protected WiringManifest wiringLocal = new WiringManifest();
	
	protected AbstractWireableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		// Blank NBT isn't read when the block entity updates, so we ensure there's always SOME data
		nbt.putBoolean("Initialised", true);
		
		if(!wiringGlobal.isEmpty())
			nbt.put("Wires", WiringManifest.CODEC.encodeStart(NbtOps.INSTANCE, wiringGlobal).getOrThrow());
		
		if(!wiringLocal.isEmpty())
			nbt.put("LocalWires", WiringManifest.CODEC.encodeStart(NbtOps.INSTANCE, wiringLocal).getOrThrow());
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		wiringGlobal.clear();
		if(nbt.contains("Wires", NbtElement.COMPOUND_TYPE))
			wiringGlobal = WiringManifest.CODEC.parse(NbtOps.INSTANCE, nbt.get("Wires")).getOrThrow();
		
		wiringLocal.clear();
		if(nbt.contains("LocalWires", NbtElement.COMPOUND_TYPE))
			wiringLocal = WiringManifest.CODEC.parse(NbtOps.INSTANCE, nbt.get("LocalWires")).getOrThrow();
		
		markDirty();
	}
	
	/** Returns true if the given input port is active */
	public final boolean getInput(Port port) 
	{
		return getPortStatus(port, true);
	}
	
	/** Returns true if the given output port is active */
	public final boolean getOutput(Port port) 
	{
		return getPortStatus(port, false);
	}
	
	protected boolean getPortStatus(Port port, boolean isInput)
	{
		Optional<ManifestEntry> global = wiringGlobal.getPort(port, isInput);
		if(global.isPresent() && global.get().status(getWorld(), BlockPos.ORIGIN))
			return true;
		
		Optional<ManifestEntry> local = wiringLocal.getPort(port, isInput);
		if(local.isPresent() && local.get().status(getWorld(), getPos()))
			return true;
		
		return false;
	}
	
	public final boolean hasInputs() { return wiringGlobal.totalInputs() + wiringLocal.totalInputs() > 0; }
	public final boolean hasOutputs() { return wiringGlobal.totalOutputs() + wiringLocal.totalOutputs() > 0; }
	
	protected IWireableBlock getWireable()
	{
		return (IWireableBlock)getCachedState().getBlock();
	}
	
	public final List<BlockPos> getInputListeners()
	{
		List<BlockPos> set = Lists.newArrayList();
		final Consumer<BlockPos> registrar = p -> 
		{
			if(p != null && set.stream().noneMatch(p2 -> p2.getManhattanDistance(p) == 0))
				set.add(p);
		};
		
		for(Port port : getWireable().inputPorts(getPos(), getWorld()))
		{
			wiringGlobal.getInputListeners(port, BlockPos.ORIGIN).forEach(registrar);
			wiringLocal.getInputListeners(port, getPos()).forEach(registrar);
		}
		
		return set;
	}
	
	public final List<BlockPos> getOutputListeners()
	{
		List<BlockPos> set = Lists.newArrayList();
		final Consumer<BlockPos> registrar = p -> 
		{
			if(set.stream().noneMatch(p2 -> p2.getManhattanDistance(p) == 0))
				set.add(p);
		};
		
		for(Port port : getWireable().outputPorts(getPos(), getWorld()))
		{
			wiringGlobal.getOutputListeners(port, BlockPos.ORIGIN).forEach(registrar);
			wiringLocal.getOutputListeners(port, getPos()).forEach(registrar);
		}
		return set;
	}
	
	public final int wireCount() { return wiringGlobal.size() + wiringLocal.size(); }
	
	public abstract boolean processInputConnection(Port input, PortEntry output, WireMode space);
	
	public abstract boolean processOutputConnection(Port output, PortEntry input, WireMode space);
	
	public void reset()
	{
		// Deactivate any attached actors
		for(Port port : getWireable().outputPorts(getPos(), getWorld()))
		{
			wiringGlobal.getOutputListeners(port, BlockPos.ORIGIN).forEach(p -> IWireableBlock.getWireable(p, world).ifPresent(b -> b.respondToPorts(p, world)));
			wiringLocal.getOutputListeners(port, getPos()).forEach(p -> IWireableBlock.getWireable(p, world).ifPresent(b -> b.respondToPorts(p, world)));
		}
		
		// Clear connections
		wiringGlobal.clear();
		wiringLocal.clear();
		
		// Deactivate self
		resetBlock();
		
		markDirty();
	}
	
	protected abstract void resetBlock();
	
	protected void cleanActors() { cleanAllOf(false); }
	
	protected void cleanSensors() { cleanAllOf(true); }
	
	protected void cleanAllOf(boolean isInputs)
	{
		boolean result = false;
		if(isInputs)
		{
			result = wiringGlobal.cleanInputsGlobal(getWorld());
			result = wiringLocal.cleanInputsLocal(getWorld(), getPos()) || result;
		}
		else
		{
			result = wiringGlobal.cleanOutputsGlobal(getWorld());
			result = wiringLocal.cleanOutputsLocal(getWorld(), getPos()) || result;
		}
		if(result)
			markDirty();
	}
	
	protected final void addInputWire(Port input, PortEntry port, WireMode space)
	{
		switch(space)
		{
			case GLOBAL:
				wiringGlobal.getPort(input, true).ifPresent(m -> m.attach(port));
				break;
			case LOCAL:
				wiringLocal.getPort(input, true).ifPresent(m -> m.attach(port.relativeTo(getPos())));
				break;
		}
		markDirty();
	}
	
	protected final void addOutputWire(Port output, PortEntry port, WireMode space)
	{
		switch(space)
		{
			case GLOBAL:
				wiringGlobal.getPort(output, false).ifPresent(m -> m.attach(port));
				break;
			case LOCAL:
				wiringLocal.getPort(output, false).ifPresent(m -> m.attach(port.relativeTo(getPos())));
				break;
		}
		markDirty();
	}
	
	public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
	
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registries) { return createNbt(registries); }
	
	public void markDirty()
	{
		if(world != null)
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}
}
