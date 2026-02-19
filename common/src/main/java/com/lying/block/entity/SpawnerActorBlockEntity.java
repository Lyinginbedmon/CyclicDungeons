package com.lying.block.entity;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.SpawnerActorBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.collection.DataPool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

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
							tile.logic.spawnNextEntry((ServerWorld)world, pos, state.get(SpawnerActorBlock.FACING));
							tile.tickCount++;
						}
						break;
					case CONSTANT:
						if(tile.tickCount++%tile.spawnRate == 0)
							tile.logic.spawnNextEntry((ServerWorld)world, pos, state.get(SpawnerActorBlock.FACING));
						break;
				}
			}
		}
		else
			tile.tickCount = 0;
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
	
	public static class TrapSpawnerLogic
	{
		@Nullable
		protected MobSpawnerEntry spawnEntry;
		protected DataPool<MobSpawnerEntry> spawnPotentials = DataPool.<MobSpawnerEntry>empty();
		protected int spawnCount = 4;
		protected int spawnRange = 4;
		
		public void readNbt(NbtCompound nbt)
		{
			if(nbt.contains("SpawnData", NbtElement.COMPOUND_TYPE))
				setSpawnEntry(MobSpawnerEntry.CODEC
						.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData"))
						.resultOrPartial(s -> CyclicDungeons.LOGGER.warn("Invalid SpawnData: {}", s))
						.orElseGet(MobSpawnerEntry::new));
			
			if(nbt.contains("SpawnPotentials", NbtElement.LIST_TYPE))
			{
				NbtList list = nbt.getList("SpawnPotentials", NbtElement.COMPOUND_TYPE);
				this.spawnPotentials = MobSpawnerEntry.DATA_POOL_CODEC
						.parse(NbtOps.INSTANCE, list)
						.resultOrPartial(e -> CyclicDungeons.LOGGER.warn("Invalid SpawnPotentials list: {}", e))
						.orElseGet(() -> DataPool.<MobSpawnerEntry>empty());
			}
			else
				this.spawnPotentials = DataPool.of(this.spawnEntry != null ? this.spawnEntry : new MobSpawnerEntry());
			
			if(nbt.contains("SpawnRange", NbtElement.INT_TYPE))
				this.spawnRange = nbt.getShort("SpawnRange");
			
			if(nbt.contains("SpawnCount", NbtElement.INT_TYPE))
				this.spawnCount = nbt.getShort("SpawnCount");
		}
		
		public NbtCompound writeNbt(NbtCompound nbt)
		{
			nbt.putShort("SpawnCount", (short)spawnCount);
			nbt.putShort("SpawnRange", (short)spawnRange);
			if(this.spawnEntry != null)
				nbt.put("SpawnData", MobSpawnerEntry.CODEC.encodeStart(NbtOps.INSTANCE, spawnEntry).getOrThrow(s -> new IllegalStateException("Invalid SpawnData: " + s)));
			nbt.put("SpawnPotentials", MobSpawnerEntry.DATA_POOL_CODEC.encodeStart(NbtOps.INSTANCE, spawnPotentials).getOrThrow());
			return nbt;
		}
		
		public void sendStatus(World world, BlockPos pos, int status)
		{
			world.addSyncedBlockEvent(pos, Blocks.SPAWNER, status, 0);
		}
		
		public void clientTick(World world, BlockPos pos)
		{
			Random random = world.getRandom();
			double d = (double)pos.getX() + random.nextDouble();
			double e = (double)pos.getY() + random.nextDouble();
			double f = (double)pos.getZ() + random.nextDouble();
			world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
			world.addParticle(ParticleTypes.FLAME, d, e, f, 0.0, 0.0, 0.0);
		}
		
		public void setEntityId(EntityType<?> type, @Nullable World world, Random random, BlockPos pos)
		{
			getSpawnEntry(world, random, pos).getNbt().putString("id", Registries.ENTITY_TYPE.getId(type).toString());
		}
		
		protected MobSpawnerEntry getSpawnEntry(@Nullable World world, Random random, BlockPos pos)
		{
			if(this.spawnEntry != null)
				return this.spawnEntry;
			else
			{
				this.setSpawnEntry((MobSpawnerEntry)this.spawnPotentials.getOrEmpty(random).map(Weighted.Present::data).orElseGet(MobSpawnerEntry::new));
				return this.spawnEntry;
			}
		}
		
		protected void setSpawnEntry(MobSpawnerEntry spawnEntry)
		{
			this.spawnEntry = spawnEntry;
		}
		
		protected void spawnNextEntry(ServerWorld world, BlockPos spawnerPos, Direction ignore)
		{
			boolean dirty = false;
			final Random random = world.getRandom();
			MobSpawnerEntry mobSpawnerEntry = this.getSpawnEntry(world, random, spawnerPos);
			
			for(int i = 0; i < this.spawnCount; i++)
			{
				NbtCompound entryNBT = mobSpawnerEntry.getNbt();
				Optional<EntityType<?>> entityType = EntityType.fromNbt(entryNBT);
				if(entityType.isEmpty())
				{
					this.updateSpawns(world, spawnerPos, random);
					return;
				}
				
				// Prevent non-Peaceful mobs from spawning in Peaceful difficulty
				if(world.getDifficulty() == Difficulty.PEACEFUL && !entityType.get().getSpawnGroup().isPeaceful())
				{
					this.updateSpawns(world, spawnerPos, random);
					return;
				}
				
				// Attempt to find a spawnable position
				Optional<MobSpawnerEntry.CustomSpawnRules> customSpawnRules = mobSpawnerEntry.getCustomSpawnRules();
				/** Predicate defining any spawn rules for this entry */
				final Predicate<BlockPos> spawnRules = blockPos -> 
				{
					if(customSpawnRules.isPresent() && !customSpawnRules.get().canSpawn(blockPos, world))
						return false;
					if(!SpawnRestriction.canSpawn((EntityType<?>)entityType.get(), world, SpawnReason.SPAWNER, blockPos, world.getRandom()))
						return false;
					return true;
				};
				/** Set of any predefined coordinates */
				NbtList coordinates = entryNBT.getList("Pos", NbtElement.DOUBLE_TYPE);
				final BlockPos minimum = new BlockPos(-1, -1, -1).offset(ignore.getOpposite());
				final BlockPos maximum = new BlockPos(1, 1, 1).offset(ignore.getOpposite());
				Optional<BlockPos> spawnPos;
				int attempts = 10;
				do
				{
					// Retrieve or calculate positions within range of the spawner
					double posX = getOrMakeCoordinate(0, coordinates, BlockPos::getX, spawnerPos, minimum, maximum, 0.5D, random);
					double posY = getOrMakeCoordinate(1, coordinates, BlockPos::getY, spawnerPos, minimum, maximum, 0D, random);
					double posZ = getOrMakeCoordinate(2, coordinates, BlockPos::getZ, spawnerPos, minimum, maximum, 0.5D, random);
					
					// Check if we could spawn the mob at this position
					final BlockPos pos = BlockPos.ofFloored(posX, posY, posZ);
					spawnPos = world.isSpaceEmpty(entityType.get().getSpawnBox(posX, posY, posZ)) && spawnRules.test(pos) ? Optional.of(pos) : Optional.empty();
				}
				while(spawnPos.isEmpty() && attempts-- > 0);
				
				// If we failed to find a spawn position, try to use the spawner's position directly
				if(spawnPos.isEmpty() && spawnRules.test(spawnerPos))
					spawnPos = Optional.of(spawnerPos);
				else
					return;
				
				// Generate the entity to spawn
				BlockPos blockPos = spawnPos.get();
				Entity entity = EntityType.loadEntityWithPassengers(entryNBT, world, SpawnReason.SPAWNER, e -> 
				{
					e.refreshPositionAndAngles(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, e.getYaw(), e.getPitch());
					return e;
				});
				if(entity == null)
				{
					this.updateSpawns(world, spawnerPos, random);
					return;
				}
				
				entity.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0F, 0.0F);
				
				// Populate mob equipment
				if(entity instanceof MobEntity mobEntity)
				{
					if(mobSpawnerEntry.getCustomSpawnRules().isEmpty() && !mobEntity.canSpawn(world, SpawnReason.SPAWNER) || !mobEntity.canSpawn(world))
						continue;
					
					// If no complex entity data present, equip according to local difficulty
					if(mobSpawnerEntry.getNbt().getSize() == 1 && mobSpawnerEntry.getNbt().contains("id", 8))
						((MobEntity)entity).initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.SPAWNER, null);
					
					mobSpawnerEntry.getEquipment().ifPresent(mobEntity::setEquipmentFromTable);
				}
				
				// Actually spawn the entity
				if(!world.spawnNewEntityAndPassengers(entity))
				{
					this.updateSpawns(world, spawnerPos, random);
					return;
				}
				
				world.syncWorldEvent(2004, spawnerPos, 0);
				world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
				if(entity instanceof MobEntity)
					((MobEntity)entity).playSpawnEffects();
				
				dirty = true;
			}
			
			if (dirty)
				this.updateSpawns(world, spawnerPos, random);
		}
		
		protected static double getOrMakeCoordinate(int index, NbtList coordinates, Function<BlockPos,Integer> getter, BlockPos spawnerPos, BlockPos minimum, BlockPos maximum, double offset, Random random)
		{
			if(coordinates.size() >= (index + 1))
				return coordinates.getDouble(index);
			
			int min = Math.max(getter.apply(minimum), -1);
			int max = Math.min(getter.apply(maximum), 1);
			return getter.apply(spawnerPos) + offset + min + ((max - min) * random.nextDouble());
		}
		
		/** Sets a random option from the spawn potentials list as the current spawn entry */
		protected void updateSpawns(World world, BlockPos pos, Random random)
		{
			this.spawnPotentials.getOrEmpty(random).ifPresent(spawnPotential -> this.setSpawnEntry((MobSpawnerEntry)spawnPotential.data()));
			world.addSyncedBlockEvent(pos, Blocks.SPAWNER, 1, 0);
		}
	}
}
