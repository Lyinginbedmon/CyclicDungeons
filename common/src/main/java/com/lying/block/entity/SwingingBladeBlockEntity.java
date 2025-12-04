package com.lying.block.entity;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.SwingingBladeBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SwingingBladeBlockEntity extends TrapActorBlockEntity
{
	private static final double PIVOT_OFFSET	= 3D / 16D;
	private static final double BLADE_INNER = 1.5D;
	private static final double BLADE_OUTER	= 2.8D;
	private static final float BLADE_ARC	= (float)Math.toRadians(4D);
	
	// How long a swing takes to complete
	private int swingTime = Reference.Values.TICKS_PER_SECOND;
	// The current position of the swing
	private float swingPosition = -1F;
	// The direction of the current swing
	private float swingTarget = -1F;
	// Whether the block was activated in the previous tick
	private boolean prevPower = false;
	
	// Client-side interpolation value
	public float clientSwingPosition = 0F;
	
	public SwingingBladeBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SWINGING_BLADE.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putInt("Speed", swingTime);
		nbt.putFloat("Swing", swingPosition());
		nbt.putFloat("SwingTarget", swingTarget);
		nbt.putBoolean("PrevPower", prevPower);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		swingTime = nbt.getInt("Speed");
		swingPosition = nbt.getFloat("Swing");
		swingTarget = nbt.getFloat("SwingTarget");
		prevPower = nbt.getBoolean("PrevPower");
	}
	
	public float swingPosition() { return MathHelper.clamp(swingPosition, -1F, 1F); }
	
	public boolean swingComplete() { return swingPosition() == swingTarget; }
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type != WireRecipient.ACTOR)
			addWire(pos, type);
		return type != WireRecipient.ACTOR;
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SWINGING_BLADE.get() ? 
				null : 
				SwingingBladeBlock.validateTicker(type, CDBlockEntityTypes.SWINGING_BLADE.get(), 
					world.isClient() ? 
						SwingingBladeBlockEntity::tickClient : 
						SwingingBladeBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, SwingingBladeBlockEntity tile)
	{
		TrapActorBlockEntity.tickClient(world, pos, state, tile);
		
		if(tile.clientSwingPosition != tile.swingPosition())
			tile.clientSwingPosition += (tile.swingPosition() - tile.clientSwingPosition) * 0.9F;
		
		if(tile.swingComplete() || world.getRandom().nextBoolean())
			return;
		
		final Vec3d pivot = getPivotPoint(pos, state.get(SwingingBladeBlock.FACING));
		Vec3d centre = getBladeOffset(state, (float)Math.toRadians(90D * tile.clientSwingPosition));
		Vec3d outer = pivot.add(centre.multiply((BLADE_OUTER + BLADE_INNER) * 0.5D));
		world.addParticle(ParticleTypes.CRIT, outer.x, outer.y, outer.z, 0, 0, 0);
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, SwingingBladeBlockEntity tile)
	{
		TrapActorBlockEntity.tickServer(world, pos, state, tile);
		
		boolean dirty = false;
		boolean power = state.get(SwingingBladeBlock.POWERED);
		if(!tile.swingComplete())
		{
			float delta = Math.signum(tile.swingTarget - tile.swingPosition());
			tile.swingPosition += (2F / tile.swingTime) * delta;
			dirty = true;
		}
		// When power is engaged, switch swing target once
		else if(power && !tile.prevPower)
			tile.switchSwing();
		
		if(power != tile.prevPower)
		{
			tile.prevPower = power;
			dirty = true;
		}
		
		if(dirty)
			tile.markDirty();
		
		// Calculate bracket pivot point
		final Direction facing = state.get(SwingingBladeBlock.FACING);
		final Vec3d pivot = getPivotPoint(pos, facing);
		
		// Calculate direction to blade
		final float bladeAngle = (float)tile.getBladeAngle();
		
		List<LivingEntity> hits = Lists.newArrayList();
		for(Vec3d point : new Vec3d[] 
					{
						getBladeOffset(state, bladeAngle + BLADE_ARC), 
						getBladeOffset(state, bladeAngle), 
						getBladeOffset(state, bladeAngle - BLADE_ARC)
					})
		{
			// Draw traces from inner to outer edge of blade relative to pivot point
			Vec3d inner = pivot.add(point.multiply(BLADE_INNER));
			Vec3d outer = pivot.add(point.multiply(BLADE_OUTER));
			
			EntityHitResult trace = ProjectileUtil.getEntityCollision(world, null, inner, outer, new Box(inner, outer), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
			if(trace == null || trace.getType() != Type.ENTITY || !(trace.getEntity() instanceof LivingEntity))
				continue;
			
			LivingEntity entity;
			if(!hits.contains(entity = (LivingEntity)trace.getEntity()))
				hits.add(entity);
		}
		
		// Damage all entities hit by the traces and apply directional knockback
		hits.forEach(tile::hitEntity);
	}
	
	protected static Vec3d getPivotPoint(BlockPos pos, Direction facing)
	{
		return new Vec3d(pos.getX(), pos.getY(), pos.getZ())
				.add(0.5D)
				.add(
					facing.getOpposite().getOffsetX() * PIVOT_OFFSET, 
					facing.getOpposite().getOffsetY() * PIVOT_OFFSET, 
					facing.getOpposite().getOffsetZ() * PIVOT_OFFSET);
	}
	
	/** Returns the current blade angle in radians */
	protected double getBladeAngle()
	{
		return Math.toRadians(90D * swingPosition);
	}
	
	protected static Vec3d getBladeOffset(BlockState state, float angle)
	{
		Direction facing = state.get(SwingingBladeBlock.FACING);
		Direction.Axis axis = state.get(SwingingBladeBlock.AXIS);
		
		// Blade direction at 0 radians
		Vec3d forward = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
		if(facing.getAxis() == axis)
			return forward;
		
		// Rotate angle to match block entity rendering
		switch(facing.getAxis())
		{
			case X:
				if(axis == Direction.Axis.Y)
					return forward.rotateZ(angle * (facing == Direction.EAST ? 1 : -1));
				else
					return forward.rotateY(angle);
			case Y:
				if(axis == Direction.Axis.X)
					return forward.rotateZ(angle * (facing == Direction.UP ? 1 : -1));
				else
					return forward.rotateX(-angle);
			case Z:
				if(axis == Direction.Axis.Y)
					return forward.rotateX(angle * (facing == Direction.NORTH ? 1 : -1));
				else
					return forward.rotateY(angle * (facing == Direction.NORTH ? -1 : 1));
			default:
				return forward;
		}
	}
	
	public void hitEntity(LivingEntity entity)
	{
		if(!entity.damage((ServerWorld)getWorld(), getWorld().getDamageSources().generic(), 6F))
			return;
		
		// Apply knockback in direction of swing
	}
	
	public void switchSwing()
	{
		swingTarget = swingTarget == 1F ? -1F : 1F;
		markDirty();
	}
}
