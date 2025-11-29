package com.lying.block.entity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import com.lying.block.SightSensorBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class SightSensorBlockEntity extends BlockEntity
{
	public static final Predicate<Entity> IS_VISIBLE = EntityPredicates.EXCEPT_SPECTATOR.and(e -> !e.isInvisible());
	
	/** How far the eye can see, ie. the height of its view cone */
	protected double sightRange = 8D;
	/** How far at most from the look direction the eye can see, ie. the radius of the view cone at its base */
	protected double sightRadius = 4D;
	
	/** UUID of the player being tracked, if any */
	private Optional<UUID> lookTargetPlayer = Optional.empty();
	/** Direction the eye is looking currently */
	private Vec3d lookVec = new Vec3d(0, 0, -1);
	private int tickCount = 0;
	
	/** Client look direction, interpolated between the last look vec and the current one */
	public Vec3d clientLookVec = new Vec3d(0, 0, -1);
	
	public SightSensorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SIGHT_SENSOR.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putDouble("Range", sightRange);
		nbt.putDouble("Radius", sightRadius);
		
		lookTargetPlayer.ifPresent(id -> nbt.putUuid("Target", id));
		nbt.putDouble("LookX", lookVec.x);
		nbt.putDouble("LookY", lookVec.y);
		nbt.putDouble("LookZ", lookVec.z);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		sightRange = Math.max(1D, nbt.getDouble("Range"));
		sightRadius = Math.max(0.5D, nbt.contains("Radius") ? nbt.getDouble("Radius") : sightRange * 0.5D);
		
		lookTargetPlayer = nbt.contains("Target") ? Optional.of(nbt.getUuid("Target")) : Optional.empty();
		lookVec = new Vec3d(
				nbt.getDouble("LookX"),
				nbt.getDouble("LookY"),
				nbt.getDouble("LookZ")
				).normalize();
	}
	
	public Vec3d currentLook() { return this.lookVec; }
	
	public Optional<UUID> currentTarget() { return this.lookTargetPlayer; }
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SIGHT_SENSOR.get() ? 
				null : 
				SightSensorBlock.validateTicker(type, CDBlockEntityTypes.SIGHT_SENSOR.get(), 
					world.isClient() ? 
						SightSensorBlockEntity::tickClient : 
						SightSensorBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, SightSensorBlockEntity tile)
	{
		Vec3d look = tile.currentLook();
		double adjustRate = 0.1D;
		
		PlayerEntity player;
		if(tile.currentTarget().isPresent() && (player = world.getPlayerByUuid(tile.currentTarget().get())) != null)
		{
			look = player.getEyePos().subtract(tile.getEyePos()).normalize();
			adjustRate = 0.5D;
		}
		
		tile.clientLookVec = tile.clientLookVec.add(look.subtract(tile.clientLookVec).multiply(adjustRate));
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, SightSensorBlockEntity tile)
	{
		if(tile.tickCount++%updateRate(tile) > 0)
			return;
		
		if(tile.lookTargetPlayer.isEmpty())
		{
			// Try to find player nearest to latest look direction to look at, otherwise randomly adjust look direction
			final Vec3d lookVec = tile.lookVec;
			
			Optional<ServerPlayerEntity> newTarget = ((ServerWorld)world).getPlayers().stream()
					.filter(IS_VISIBLE)
					.filter(tile::isInViewCone)
					.filter(p -> canEyeSeePlayer(tile, p, world))
					.sorted((a,b) -> 
					{
						double distA = tile.distanceFromLook(a.getEyePos());
						double distB = tile.distanceFromLook(b.getEyePos());
						return distA < distB ? -1 : distA > distB ? 1 : 0;
					}).findFirst();
			
			newTarget.ifPresentOrElse(p -> 
			{
				tile.startTracking(p);
				tile.markDirty();
			}, () -> 
			{
				Random rand = world.getRandom();
				tile.lookVec = lookVec.add(new Vec3d(
						rand.nextDouble() - 0.5D,
						rand.nextDouble() - 0.5D,
						rand.nextDouble() - 0.5D
						)).normalize();
				tile.lookVec = new Vec3d(tile.lookVec.x, MathHelper.clamp(tile.lookVec.y, -0.3, 0.3), tile.lookVec.z).normalize();
				
				tile.markDirty();
			});
		}
		else if(tile.lookTargetPlayer.isPresent())
		{
			// Try to look at player, if fail then stop following and set as looking in last valid direction
			PlayerEntity player = world.getPlayerByUuid(tile.lookTargetPlayer.get());
			if(player == null)
			{
				tile.clearTracking();
				tile.markDirty();
				return;
			}
			
			if(player != null && IS_VISIBLE.test(player) && canEyeSeePlayer(tile, player, world))
			{
				int charge = tile.getCachedState().get(SightSensorBlock.POWER);
				if(charge < 15)
					world.setBlockState(tile.getPos(), state.with(SightSensorBlock.POWER, ++charge), 3);
			}
			else
			{
				tile.stopTracking(player);
				tile.markDirty();
			}
		}
	}
	
	/** Called when the eye starts tracking a player */
	protected void startTracking(PlayerEntity player)
	{
		lookTargetPlayer = Optional.of(player.getUuid());
		updateBlock(1);
	}
	
	/** Called when the targeted player is no longer visible */
	protected void stopTracking(PlayerEntity player)
	{
		if(lookTargetPlayer.isEmpty() || !lookTargetPlayer.get().equals(player.getUuid()))
			return;
		
		lookTargetPlayer = Optional.empty();
		lookVec = player.getEyePos().subtract(getEyePos()).normalize();
		tickCount = 0;
		updateBlock(0);
	}
	
	/** Called when the targeted player no longer exists, not just isn't visible */
	protected void clearTracking()
	{
		lookTargetPlayer = Optional.empty();
		lookVec = new Vec3d(0, 0, -1);
		updateBlock(0);
	}
	
	protected void updateBlock(int power)
	{
		if(power == getCachedState().get(SightSensorBlock.POWER))
			return;
		
		BlockState state = getCachedState().with(SightSensorBlock.POWER, power).with(SightSensorBlock.POWERED, power > 0);
		world.setBlockState(getPos(), state, 3);
		world.updateListeners(getPos(), getCachedState(), state, 3);
	}
	
	public Vec3d getEyePos()
	{
		BlockPos tilePos = getPos();
		return new Vec3d(tilePos.getX(), tilePos.getY(), tilePos.getZ()).add(0.5D);
	}
	
	/** Returns the distance to the point from its equivalent point within the eye's view cone */
	public double distanceFromLook(Vec3d p)
	{
		double distToP = getEyePos().distanceTo(p);
		Vec3d conePoint = getEyePos().add(this.lookVec.multiply(distToP));
		return p.distanceTo(conePoint);
	}
	
	/** Returns true if the player's eye position is within this eye's view cone, ignoring obstructions */
	protected boolean isInViewCone(PlayerEntity player)
	{
		double distFromEye = getEyePos().distanceTo(player.getEyePos());
		double radiusAtDist = (distFromEye / this.sightRange) * this.sightRadius;
		return distFromEye <= this.sightRange && distanceFromLook(player.getEyePos()) < radiusAtDist;
	}
	
	/**
	 * Returns true if the eye has unobstructed line of sight to the given player
	 */
	protected static boolean canEyeSeePlayer(SightSensorBlockEntity eye, PlayerEntity player, World world)
	{
		if(eye.getEyePos().distanceTo(player.getEyePos()) > eye.sightRange)
			return false;
		
		BlockHitResult trace = world.raycast(new RaycastContext(player.getEyePos(), eye.getEyePos(), RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, ShapeContext.absent()));
		return trace.getType() == Type.MISS || trace.getBlockPos().getManhattanDistance(eye.getPos()) == 0;
	}
	
	public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
	
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registries) { return createNbt(registries); }
	
	public void markDirty()
	{
		if(world != null)
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}
	
	public static int updateRate(SightSensorBlockEntity tile)
	{
		return tile.currentTarget().isPresent() ? Reference.Values.TICKS_PER_SECOND : Reference.Values.TICKS_PER_SECOND * 2;
	}
}
