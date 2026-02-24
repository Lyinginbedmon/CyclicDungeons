package com.lying.block.entity;

import org.joml.Vector3i;

import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.SpawnerActorBlock;
import com.lying.init.CDBlockEntityTypes;
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

public class SpawnerActorBlockEntity extends TrapActorBlockEntity
{
	private TrapSpawnerLogic logic = new TrapSpawnerLogic();
	private ActivationType activation = ActivationType.CONSTANT;
	private int spawnRate = Reference.Values.TICKS_PER_SECOND;
	private int tickCount = 0;
	private boolean prevPower = false;
	
	public SpawnerActorBlockEntity(BlockPos pos, BlockState state)
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
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type != WireRecipient.ACTOR)
			addWire(pos, type);
		return type != WireRecipient.ACTOR;
	}
	
	public void setSpawnEntries(TrapSpawnerLogic logicIn)
	{
		logic = logicIn;
		this.markDirty();
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SPAWNER.get() ? 
				null : 
				SpawnerActorBlock.validateTicker(type, CDBlockEntityTypes.SPAWNER.get(), 
					world.isClient() ? 
						SpawnerActorBlockEntity::tickClient : 
						SpawnerActorBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, SpawnerActorBlockEntity tile)
	{
		TrapActorBlockEntity.tickClient(world, pos, state, tile);
		
		Random rand = world.getRandom();
		boolean power = state.get(SpawnerActorBlock.POWERED);
		if(power)
		{
			if(!tile.prevPower)
				world.playSoundAtBlockCenter(pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1, 0.75F + rand.nextFloat() * 0.2F, true);
			
			Direction facing = state.get(SpawnerActorBlock.FACING);
			tile.logic.clientTick(world, pos);
			
		}
		else if(tile.prevPower)
		{
			
		}
		
		tile.prevPower = power;
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, SpawnerActorBlockEntity tile)
	{
		TrapActorBlockEntity.tickServer(world, pos, state, tile);
		
		if(state.get(SpawnerActorBlock.POWERED))
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
							tile.logic.spawnNextEntry((ServerWorld)world, pos, tile.getSpawningArea());
							tile.tickCount++;
						}
						break;
					case CONSTANT:
						if(tile.tickCount++%tile.spawnRate == 0)
							tile.logic.spawnNextEntry((ServerWorld)world, pos, tile.getSpawningArea());
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
	public Box getSpawningArea()
	{
		final Direction facing = getCachedState().get(SpawnerActorBlock.FACING).getOpposite();
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
