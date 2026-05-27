package com.lying.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ITrapActor extends IWireableBlock
{
	public void trigger(BlockPos pos, World world);
	
	public default void activate(BlockPos pos, World world) { if(!isActive(pos, world)) trigger(pos, world); }
	public default void deactivate(BlockPos pos, World world) { if(isActive(pos, world)) trigger(pos, world); }
}
