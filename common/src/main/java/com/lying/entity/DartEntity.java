package com.lying.entity;

import java.util.List;

import com.lying.init.CDEntityTypes;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** Essentially just an {@link ArrowEntity} whose damage is not affected by velocity and does not pierce */
public class DartEntity extends ArrowEntity
{
	public DartEntity(EntityType<? extends ArrowEntity> entityType, World world)
	{
		super(CDEntityTypes.DART.get(), world);
		this.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
	}
	
	public DartEntity(World world, double x, double y, double z, ItemStack stack)
	{
		this(CDEntityTypes.DART.get(), world);
		setStack(stack.copy());
		setCustomName(stack.get(DataComponentTypes.CUSTOM_NAME));
		Unit unit = stack.remove(DataComponentTypes.INTANGIBLE_PROJECTILE);
		if(unit != null)
			this.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
		setPosition(x, y, z);
	}
	
	@SuppressWarnings("deprecation")
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		Entity entity = entityHitResult.getEntity();
		double damage = getDamage();
		Entity owner = getOwner();
		DamageSource damageSource = getDamageSources().arrow(this, (Entity)(owner != null ? owner : this));
		if(getWeaponStack() != null && getWorld() instanceof ServerWorld serverWorld)
			damage = (double)EnchantmentHelper.getDamage(serverWorld, getWeaponStack(), entity, damageSource, (float)damage);
		
		int totalDamage = MathHelper.ceil(MathHelper.clamp((double)damage, 0.0, 2.147483647E9));
		if(isCritical())
		{
			long l = (long)getRandom().nextInt(totalDamage / 2 + 2);
			totalDamage = (int)Math.min(l + (long)totalDamage, 2147483647L);
		}
		
		if(owner instanceof LivingEntity livingOwner)
			livingOwner.onAttacking(entity);
		
		boolean hitEnderman = entity.getType() == EntityType.ENDERMAN;
		int j = entity.getFireTicks();
		if(isOnFire() && !hitEnderman)
			entity.setOnFireFor(5.0F);
		
		if(entity.sidedDamage(damageSource, (float)totalDamage))
		{
			if(hitEnderman)
				return;
			
			if(entity instanceof LivingEntity livingEntity2)
			{
				if(!getWorld().isClient() && getPierceLevel() <= 0)
					livingEntity2.setStuckArrowCount(livingEntity2.getStuckArrowCount() + 1);
				
				knockback(livingEntity2, damageSource);
				if(getWorld() instanceof ServerWorld serverWorld2)
					EnchantmentHelper.onTargetDamaged(serverWorld2, livingEntity2, damageSource, getWeaponStack());
				
				onHit(livingEntity2);
				if(livingEntity2 != owner && livingEntity2 instanceof PlayerEntity && owner instanceof ServerPlayerEntity && !isSilent())
					((ServerPlayerEntity)owner).networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER, 0.0F));
				
				if(!getWorld().isClient() && owner instanceof ServerPlayerEntity serverPlayerEntity && !entity.isAlive())
					Criteria.KILLED_BY_ARROW.trigger(serverPlayerEntity, List.of(entity), getWeaponStack());
			}
			
			playSound(getSound(), 1.0F, 1.2F / (getRandom().nextFloat() * 0.2F + 0.9F));
			if(getPierceLevel() <= 0)
				discard();
		}
		else
		{
			entity.setFireTicks(j);
			deflect(ProjectileDeflection.SIMPLE, entity, getOwner(), false);
			setVelocity(getVelocity().multiply(0.2));
			if(getWorld() instanceof ServerWorld && getVelocity().lengthSquared() < 1.0E-7)
				discard();
		}
	}
}
