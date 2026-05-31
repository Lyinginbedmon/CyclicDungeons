package com.lying.block.sensors;

import java.util.List;

import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.PortEntry;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;

import net.minecraft.block.Block;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractTrapSensorBlock extends Block implements IWireableBlock
{
	public static final IntProperty POWER	= Properties.POWER;
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
	protected AbstractTrapSensorBlock(Settings settings)
	{
		super(settings.strength(50F, 0F).dropsNothing());
	}
	
	public WireRecipient type() { return WireRecipient.SENSOR; }
	
	public List<Port> inputPorts(BlockPos pos, World world) { return List.of(); }
	public List<Port> outputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.OUTPUT); }
	
	/** Sensors don't need to respond to ports because they only transmit signals */
	public void respondToPorts(BlockPos pos, World world) { }
	
	public boolean acceptWireTo(Port output, BlockPos target, WireMode space, PortEntry input, World world)
	{
		return true;
	}
	
	public boolean acceptWireFrom(Port input, BlockPos target, WireMode space, PortEntry output, World world)
	{
		return false;
	}
	
	public boolean isPortActive(Port port, BlockPos pos, World world)
	{
		return port.equals(CDLogicGates.OUTPUT);
	}
}
