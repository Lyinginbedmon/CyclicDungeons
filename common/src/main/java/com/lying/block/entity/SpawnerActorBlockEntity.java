package com.lying.block.entity;

import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class SpawnerActorBlockEntity extends TrapActorBlockEntity
{
	public SpawnerActorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SPAWNER.get(), pos, state);
	}
}
