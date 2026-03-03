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
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ProximitySensorBlockEntity extends TrapSensorBlockEntity<ProximitySensorBlockEntity>
{
	public static final Predicate<Entity> PREDICATE = EntityPredicates.EXCEPT_SPECTATOR;
	
	private static final double DEFAULT_RANGE = 8D;
	private static final Vec3d DEFAULT_AREA = new Vec3d(DEFAULT_RANGE, DEFAULT_RANGE, DEFAULT_RANGE);
	private double searchRange = DEFAULT_RANGE;
	private Vec3d searchArea = DEFAULT_AREA;
	
	private boolean shouldRenderRange = false;
	
	public ProximitySensorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.PROXIMITY_SENSOR.get(), pos, state);
	}
	
	protected void storeSettings(NbtCompound nbt)
	{
		super.storeSettings(nbt);
		
		if(searchRange > 0D)
			nbt.putDouble("Range", Math.abs(searchRange));
		else
		{
			if(searchArea.x == searchArea.y && searchArea.y == searchArea.z)
				nbt.putDouble("Area", searchArea.x);
			else
			{
				NbtList list = new NbtList();
				list.add(NbtDouble.of(searchArea.x));
				list.add(NbtDouble.of(searchArea.y));
				list.add(NbtDouble.of(searchArea.z));
				nbt.put("Area", list);
			}
		}
	}
	
	protected void loadSettings(NbtCompound nbt)
	{
		super.loadSettings(nbt);
		
		if(nbt.contains("Range", NbtElement.DOUBLE_TYPE))
		{
			searchRange = nbt.getDouble("Range");
			searchArea = Vec3d.ZERO;
		}
		else
		{
			searchRange = 0D;
			
			NbtList list;
			if(nbt.contains("Area", NbtElement.DOUBLE_TYPE))
			{
				double v = nbt.getDouble("Area");
				searchArea = new Vec3d(v, v, v);
			}
			else if(nbt.contains("Area", NbtElement.LIST_TYPE) && (list = nbt.getList("Range", NbtElement.DOUBLE_TYPE)).size() == 3)
				searchArea = new Vec3d(list.getDouble(0), list.getDouble(1), list.getDouble(2));
			else
				searchArea = DEFAULT_AREA;
		}
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.PROXIMITY_SENSOR.get() ? 
				null : 
				ProximitySensorBlock.validateTicker(type, CDBlockEntityTypes.PROXIMITY_SENSOR.get(), 
					world.isClient() ? 
						TrapSensorBlockEntity::tickClient : 
						TrapSensorBlockEntity::tickServer);
	}
	
	public void setSearchRange(double range)
	{
		this.searchRange = range;
		this.searchArea = Vec3d.ZERO;
		markDirty();
	}
	
	public void setSearchArea(Vec3d range)
	{
		this.searchArea = range;
		this.searchRange = 0D;
		markDirty();
	}
	
	public double getSearchRange() { return searchRange; }
	
	public boolean shouldBeActive(ProximitySensorBlockEntity tile)
	{
		return tile.getClosestPlayer().isPresent();
	}
	
	public void runActive(ProximitySensorBlockEntity tile)
	{
		tile.getClosestPlayer().ifPresentOrElse(p -> 
			ProximitySensorBlock.setPower((int)MathHelper.clamp(15 - Math.floor(p), 0, 15), tile.getPos(), tile.getWorld()), () -> 
			ProximitySensorBlock.setPower(0, tile.getPos(), tile.getWorld()));
	}
	
	protected Optional<Double> getClosestPlayer()
	{
		return ((ServerWorld)world).getPlayers().stream()
				.filter(PREDICATE)
				.filter(this::isInRange)
				.map(this::distTo)
				.sorted()
				.findFirst();
	}
	
	public boolean isInRange(PlayerEntity player)
	{
		Vec3d dist = vecPos().subtract(player.getPos());
		if(searchRange > 0D)
			return dist.length() <= searchRange;
		else
			return 
					Math.abs(dist.x) <= Math.abs(searchArea.x) &&
					Math.abs(dist.y) <= Math.abs(searchArea.y) &&
					Math.abs(dist.z) <= Math.abs(searchArea.z);
	}
	
	public Vec3d vecPos() { return new Vec3d(getPos().getX(), getPos().getY(), getPos().getZ()).add(0.5D); }
	
	public double distTo(PlayerEntity player)
	{
		return vecPos().distanceTo(player.getPos());
	}
	
	public boolean shouldRenderFor(PlayerEntity player)
	{
		if(!shouldRenderRange)
			return false;
		if(searchRange <= 0D)
			return false;
		return vecPos().subtract(player.getPos()).length() <= searchRange + 3D;
	}
}
