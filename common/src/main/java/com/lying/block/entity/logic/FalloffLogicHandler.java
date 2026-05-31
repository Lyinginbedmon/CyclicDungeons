package com.lying.block.entity.logic;

import java.util.List;
import java.util.Optional;

import com.lying.block.ITrapActor;
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
	
	public void handleLogic(int inputState, List<BlockPos> actors, ServerWorld world)
	{
		boolean status = inputState > 0;
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
		
		for(BlockPos p : actors)
		{
			Optional<IWireableBlock> wireable = IWireableBlock.getWireable(p, world);
			if(wireable.isEmpty())
				continue;
			
			final ITrapActor trActor = (ITrapActor)wireable.get();
			if(status)
				trActor.activate(p, world);
			else
				trActor.deactivate(p, world);
		}
	}
}