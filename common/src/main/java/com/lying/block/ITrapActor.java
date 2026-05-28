package com.lying.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ITrapActor extends IWireableBlock
{
	/** Returns TRUE if the trap is currently active */
	public boolean isActive(BlockPos pos, World world);
	
	/** Triggers the trap if {@link isActive} is FALSE */
	public default void activate(BlockPos pos, World world) { if(!isActive(pos, world)) trigger(pos, world); }
	/** Triggers the trap if {@link isActive} is TRUE */
	public default void deactivate(BlockPos pos, World world) { if(isActive(pos, world)) trigger(pos, world); }
	
	public void trigger(BlockPos pos, World world);
}
