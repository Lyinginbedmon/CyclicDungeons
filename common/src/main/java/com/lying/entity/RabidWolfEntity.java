package com.lying.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.goal.UntamedActiveTargetGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class RabidWolfEntity extends WolfEntity 
{
	public RabidWolfEntity(EntityType<? extends WolfEntity> entityType, World world)
	{
		super(entityType, world);
	}
	
	protected void initGoals()
	{
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(1, new TameableEntity.TameableEscapeDangerGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		this.goalSelector.add(4, new PounceAtTargetGoal(this, 0.4F));
		this.goalSelector.add(5, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(10, new LookAroundGoal(this));
		this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		this.targetSelector.add(2, new AttackWithOwnerGoal(this));
		this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
		this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
		this.targetSelector.add(5, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE));
		this.targetSelector.add(6, new UntamedActiveTargetGoal<>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
		this.targetSelector.add(8, new UniversalAngerGoal<>(this, true));
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
