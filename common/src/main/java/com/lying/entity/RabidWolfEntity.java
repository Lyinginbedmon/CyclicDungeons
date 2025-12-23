package com.lying.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class RabidWolfEntity extends WolfEntity 
{
	public RabidWolfEntity(EntityType<? extends WolfEntity> entityType, World world)
	{
		super(entityType, world);
	}
	
	public boolean hasAngerTime() { return true; }
	
	public boolean canSpawn(WorldAccess world, SpawnReason spawnReason)
	{
		return true;
	}
	
	public boolean canSpawn(WorldView world)
	{
		return !world.containsFluid(getBoundingBox()) && world.doesNotIntersectEntities(this);
	}
}
