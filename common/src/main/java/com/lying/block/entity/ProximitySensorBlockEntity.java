package com.lying.block.entity;

import java.util.Optional;
import java.util.function.Predicate;

import com.lying.block.ProximitySensorBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ProximitySensorBlockEntity extends BlockEntity
{
	private static final Predicate<Entity> PREDICATE = EntityPredicates.EXCEPT_SPECTATOR;
	private double searchRange = 8D;
	
	public ProximitySensorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.PROXIMITY_SENSOR.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putDouble("Range", searchRange);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		searchRange = nbt.getDouble("Range");
		
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.PROXIMITY_SENSOR.get() ? 
				null : 
				ProximitySensorBlock.validateTicker(type, CDBlockEntityTypes.PROXIMITY_SENSOR.get(), 
					world.isClient() ? 
						ProximitySensorBlockEntity::tickClient : 
						ProximitySensorBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, ProximitySensorBlockEntity tile) { }
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, ProximitySensorBlockEntity tile)
	{
		Optional<Double> closest = ((ServerWorld)world).getPlayers().stream()
				.filter(p -> tile.distTo(p) <= tile.searchRange)
				.filter(PREDICATE)
				.map(tile::distTo)
				.sorted()
				.findFirst();
		
		closest.ifPresentOrElse(p -> 
			ProximitySensorBlock.setPower((int)MathHelper.clamp(p, 0, 15), pos, world), () -> 
			ProximitySensorBlock.setPower(0, pos, world));
	}
	
	public Vec3d vecPos() { return new Vec3d(getPos().getX(), getPos().getY(), getPos().getZ()).add(0.5D); }
	
	public double distTo(PlayerEntity player)
	{
		return vecPos().distanceTo(player.getPos());
	}
}
