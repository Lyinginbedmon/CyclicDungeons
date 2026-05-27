package com.lying.block.entity.logic;

import java.util.List;

import com.lying.block.ITrapActor;
import com.lying.block.IWireableBlock;
import com.lying.init.CDTrapLogicHandlers.LogicHandler;
import com.lying.reference.Reference;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CycleLogicHandler extends LogicHandler
{
	public CycleLogicHandler(Identifier idIn)
	{
		super(idIn);
	}
	
	public void handleLogic(int inputState, List<BlockPos> actors, ServerWorld world)
	{
		int index = Math.floorDiv((int)world.getTime(), Reference.Values.TICKS_PER_SECOND)%actors.size();
		for(int i=0; i<actors.size(); i++)
		{
			final BlockPos pos = actors.get(i);
			final ITrapActor trActor = (ITrapActor)IWireableBlock.getWireable(actors.get(i), world);
			if(trActor == null)
				continue;
			if(i == index)
				trActor.activate(pos, world);
			else
				trActor.deactivate(pos, world);
		}
	}
}