package com.lying.block.entity;

import org.joml.Vector3i;

import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.TrapSpawnerBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.item.WiringGunItem.WireMode;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class TrapSpawnerBlockEntity extends TrapActorBlockEntity
{
	private TrapSpawnerLogic logic = new TrapSpawnerLogic();
	private ActivationType activation = ActivationType.CONSTANT;
	private int spawnRate = Reference.Values.TICKS_PER_SECOND;
	private int tickCount = 0;
	private boolean prevPower = false;
	
	public TrapSpawnerBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SPAWNER.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		logic.writeNbt(nbt);
		nbt.putString("Activity", activation.asString());
		nbt.putShort("SpawnRate", (short)spawnRate);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		logic.readNbt(nbt);
		activation = ActivationType.fromString(nbt.getString("Activity"));
		spawnRate = nbt.getShort("SpawnRate");
	}
	
	public boolean processWireConnection(BlockPos pos, WireMode space, WireRecipient type)
	{
		if(type != WireRecipient.ACTOR)
			addWire(pos, space, type);
		return type != WireRecipient.ACTOR;
	}
	
	public TrapSpawnerLogic getLogic() { return this.logic; }
	
	public void setLogic(TrapSpawnerLogic logicIn)
	{
		this.logic = logicIn;
		markDirty();
	}
	
	public void setSpawnEntries(TrapSpawnerLogic logicIn)
	{
		logic = logicIn;
		this.markDirty();
	}
	
	public void setActivationType(ActivationType type)
	{
		this.activation = type;
		this.markDirty();
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SPAWNER.get() ? 
				null : 
				TrapSpawnerBlock.validateTicker(type, CDBlockEntityTypes.SPAWNER.get(), 
					world.isClient() ? 
						TrapSpawnerBlockEntity::tickClient : 
						TrapSpawnerBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, TrapSpawnerBlockEntity tile)
	{
		TrapActorBlockEntity.tickClient(world, pos, state, tile);
		
		Random rand = world.getRandom();
		boolean power = state.get(TrapSpawnerBlock.POWERED);
		if(power)
		{
			tile.logic.clientTick(world, pos);
			if(!tile.prevPower)
				world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1, 0.75F + rand.nextFloat() * 0.2F, true);
		}
		
		tile.prevPower = power;
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, TrapSpawnerBlockEntity tile)
	{
		TrapActorBlockEntity.tickServer(world, pos, state, tile);
		
		if(state.get(TrapSpawnerBlock.POWERED))
		{
			if(tile.logic.spawnEntry == null)
				tile.logic.setEntityId(EntityType.PIG, world, world.getRandom(), pos);
			else
			{
				switch(tile.activation)
				{
					case IMPULSE:
						if(tile.tickCount == 0)
						{
							tile.logic.spawnNextEntry((ServerWorld)world, pos, tile.getFullSpawningArea());
							tile.tickCount++;
						}
						break;
					case CONSTANT:
						if(tile.tickCount++%tile.spawnRate == 0)
							tile.logic.spawnNextEntry((ServerWorld)world, pos, tile.getFullSpawningArea());
						break;
				}
			}
		}
		else
			tile.tickCount = 0;
	}
	
	/**
	 * Returns the local coordinates area within which this spawner can spawn things<br>
	 * This is derived from the sum footprint of all directions, excepting the one the spawner is facing away from.
	 */
	public Box getBaseSpawningArea()
	{
		final Direction facing = getCachedState().get(TrapSpawnerBlock.FACING).getOpposite();
		Vector3i 
			min = new Vector3i(0,0,0), 
			max = new Vector3i(0,0,0);
		for(Direction face : Direction.values())
		{
			if(face == facing)
				continue;
			
			min = new Vector3i(
				Math.min(min.x, face.getOffsetX()),
				Math.min(min.y, face.getOffsetY()),
				Math.min(min.z, face.getOffsetZ())
					);
			max = new Vector3i(
				Math.max(max.x, face.getOffsetX()),
				Math.max(max.y, face.getOffsetY()),
				Math.max(max.z, face.getOffsetZ())
					);
		}
		
		return new Box(min.x, min.y, min.z, max.x, max.y, max.z);
	}
	
	public Box getFullSpawningArea()
	{
		Box base = getBaseSpawningArea();
		int[] range = logic.spawnRanges;
		return new Box(
				base.minX * range[0], 
				base.minY * range[1], 
				base.minZ * range[2], 
				1D + base.maxX * range[0], 
				1D + base.maxY * range[1], 
				1D + base.maxZ * range[2]);
	}
	
	public static enum ActivationType implements StringIdentifiable
	{
		IMPULSE,
		CONSTANT;

		@Override
		public String asString() { return name().toLowerCase(); }
		
		public static ActivationType fromString(String s)
		{
			for(ActivationType rate : values())
				if(rate.asString().equalsIgnoreCase(s))
					return rate;
			return IMPULSE;
		}
	}
}
