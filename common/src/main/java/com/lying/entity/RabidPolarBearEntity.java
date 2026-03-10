package com.lying.entity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class RabidPolarBearEntity extends PolarBearEntity
{
	public RabidPolarBearEntity(EntityType<? extends PolarBearEntity> entityType, World world)
	{
		super(entityType, world);
		this.experiencePoints = 5;
	}
	
	protected void initGoals()
	{
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(5, new WanderAroundGoal(this, 1.0));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.add(7, new LookAroundGoal(this));
		this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge(RabidPolarBearEntity.class));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
		this.targetSelector.add(4, new ActiveTargetGoal<>(this, FoxEntity.class, 10, true, true, null));
		this.targetSelector.add(5, new UniversalAngerGoal<>(this, true));
	}
	
	public boolean isUniversallyAngry(ServerWorld world) { return true; }
	
	public int getAngerTime() { return 1; }
	
	public boolean isAngryAt(ServerWorld world, PlayerEntity player) { return true; }
	
	@Nullable
	public UUID getAngryAt() { return null; }
	
	public boolean canTarget(LivingEntity entity) { return entity instanceof PlayerEntity && entity.canTakeDamage(); }
	
	public class PolarBearRevengeGoal extends RevengeGoal
	{
		public PolarBearRevengeGoal()
		{
			super(RabidPolarBearEntity.this);
		}
		
		public void start()
		{
			super.start();
			if(RabidPolarBearEntity.this.isBaby())
			{
				this.callSameTypeForRevenge();
				this.stop();
			}
		}
		
		protected void setMobEntityTarget(MobEntity mob, LivingEntity target)
		{
			if(mob instanceof RabidPolarBearEntity && !mob.isBaby())
				super.setMobEntityTarget(mob, target);
		}
	}
}
