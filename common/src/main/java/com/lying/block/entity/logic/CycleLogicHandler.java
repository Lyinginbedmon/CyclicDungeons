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
			Optional<IWireableBlock> wireable = IWireableBlock.getWireable(pos, world);
			if(wireable.isEmpty())
				continue;
			
			final ITrapActor trActor = (ITrapActor)wireable.get();
			if(i == index)
				trActor.activate(pos, world);
			else
				trActor.deactivate(pos, world);
		}
	}
}