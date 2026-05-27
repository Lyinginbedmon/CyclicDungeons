package com.lying.block;

import java.util.List;

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
	
	public List<String> inputPorts(BlockPos pos, World world) { return List.of(); }
	public List<String> outputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.OUTPUT); }
	
	/** Sensors don't need to respond to ports because they only transmit signals */
	public void respondToPorts(BlockPos pos, World world) { }
	
	public boolean acceptWireTo(String output, BlockPos target, WireMode space, BlockPos pos, String input, World world)
	{
		return true;
	}
	
	public boolean acceptWireFrom(String input, BlockPos target, WireMode space, BlockPos pos, String output, World world)
	{
		return false;
	}
}
