package com.lying.block.entity.logic;

import java.util.List;

import com.lying.block.IWireableBlock;
import com.lying.init.CDTrapLogicHandlers.LogicHandler;
import com.lying.reference.Reference;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class FalloffLogicHandler extends LogicHandler
{
	private final int tickDelay;
	
	public FalloffLogicHandler(Identifier id, int seconds)
	{
		super(id);
		tickDelay = Reference.Values.TICKS_PER_SECOND * seconds;
	}
	
	public void handleLogic(List<BlockPos> sensors, List<BlockPos> actors, ServerWorld world)
	{
		boolean status = sensors.stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
		if(status)
			data.putInt("Ticks", tickDelay);
		else if(data.getInt("Ticks") > 0)	// If we're in the falloff period, decrement timer
		{
			int ticksRemaining = data.getInt("Ticks");
			data.putInt("Ticks", --ticksRemaining);
			status = ticksRemaining > 0;
		}
		else
			return;	// If we're inactive and outside the falloff period, no further action is needed
		
		if(status)
			actors.forEach(p -> IWireableBlock.getWireable(p, world).activate(p, world));
		else
			actors.forEach(p -> IWireableBlock.getWireable(p, world).deactivate(p, world));
	}
}