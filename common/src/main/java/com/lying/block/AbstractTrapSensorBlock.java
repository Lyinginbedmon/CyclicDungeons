package com.lying.block;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractTrapSensorBlock extends Block implements IWireableBlock
{
	protected AbstractTrapSensorBlock(Settings settings)
	{
		super(settings);
	}
	
	public WireRecipient type() { return WireRecipient.SENSOR; }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return false;
	}
}
