package com.lying.block.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import com.lying.CyclicDungeons;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentTable;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DataPool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class TrapSpawnerLogic
{
	@Nullable
	public SpawnerEntry spawnEntry;
	protected DataPool<SpawnerEntry> spawnPotentials;
	protected int[] spawnRange = new int[] {4, 4, 4};
	
	public TrapSpawnerLogic()
	{
		spawnPotentials = DataPool.<SpawnerEntry>of(SpawnerEntry.Builder.of(EntityType.PIG).count(1).build());
	}
	
	public TrapSpawnerLogic(List<SpawnerEntry> entries)
	{
		this();
		setSpawnPotentials(entries);
	}
	
	public void readNbt(NbtCompound nbt)
	{
		if(nbt.contains("SpawnData", NbtElement.COMPOUND_TYPE))
			setSpawnEntry(SpawnerEntry.CODEC
					.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData"))
					.resultOrPartial(s -> CyclicDungeons.LOGGER.warn("Invalid SpawnData: {}", s))
					.orElseGet(SpawnerEntry::new));
		
		if(nbt.contains("SpawnPotentials", NbtElement.LIST_TYPE))
			setSpawnPotentials(nbt.get("SpawnPotentials"));
		else
			this.spawnPotentials = DataPool.of(this.spawnEntry != null ? this.spawnEntry : new SpawnerEntry());
		
		if(nbt.contains("SpawnRange", NbtElement.INT_ARRAY_TYPE))
			this.spawnRange = nbt.getIntArray("SpawnRange");
	}
	
	public NbtCompound writeNbt(NbtCompound nbt)
	{
		nbt.putIntArray("SpawnRange", spawnRange);
		if(this.spawnEntry != null)
			nbt.put("SpawnData", SpawnerEntry.CODEC.encodeStart(NbtOps.INSTANCE, spawnEntry).getOrThrow(s -> new IllegalStateException("Invalid SpawnData: " + s)));
		nbt.put("SpawnPotentials", SpawnerEntry.DATA_POOL_CODEC.encodeStart(NbtOps.INSTANCE, spawnPotentials).getOrThrow());
		return nbt;
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
		getSpawnEntry(world, random, pos).entityNBT().putString("id", Registries.ENTITY_TYPE.getId(type).toString());
	}
	
	public void setSpawnPotentials(List<SpawnerEntry> entries)
	{
		NbtList list = new NbtList();
		entries.stream()
				.map(e -> SpawnerEntry.CODEC.encodeStart(NbtOps.INSTANCE, e).getOrThrow())
				.forEach(e -> list.add(e));
		
		setSpawnPotentials(list);
	}
	
	public void setSpawnPotentials(NbtElement list)
	{
		this.spawnPotentials = SpawnerEntry.DATA_POOL_CODEC
				.parse(NbtOps.INSTANCE, list)
				.resultOrPartial(e -> CyclicDungeons.LOGGER.warn("Invalid SpawnPotentials list: {}", e))
				.orElseGet(() -> DataPool.<SpawnerEntry>empty());
	}
	
	public int getSpawnRange(Axis index) { return index.ordinal() < spawnRange.length ? spawnRange[index.ordinal()] : 0; }
	
	public void setSpawnRange(Vector3i vec)
	{
		setSpawnRange(vec.x, vec.y, vec.z);
	}
	
	protected void setSpawnRange(int x, int y, int z)
	{
		this.spawnRange = new int[] {x, y, z};
	}
	
	protected SpawnerEntry getSpawnEntry(@Nullable World world, Random random, BlockPos pos)
	{
		if(this.spawnEntry != null)
			return this.spawnEntry;
		else
		{
			this.setSpawnEntry((SpawnerEntry)this.spawnPotentials.getOrEmpty(random).map(Weighted.Present::data).orElseGet(SpawnerEntry::new));
			return this.spawnEntry;
		}
	}
	
	protected void setSpawnEntry(SpawnerEntry spawnEntry)
	{
		this.spawnEntry = spawnEntry;
	}
	
	protected void spawnNextEntry(ServerWorld world, BlockPos spawnerPos, Box spawnArea)
	{
		final Random random = world.getRandom();
		SpawnerEntry entry = this.getSpawnEntry(world, random, spawnerPos);
		
		for(int i = 0; i < entry.countToSpawn(world.getRandom()); i++)
			trySpawnEntry(entry, world, spawnerPos, spawnArea, random);
		
		this.updateSpawns(world, spawnerPos, random);
	}
	
	protected void trySpawnEntry(
			SpawnerEntry mobSpawnerEntry, 
			ServerWorld world, 
			BlockPos spawnerPos, 
			Box spawnArea, 
			Random random)
	{
		NbtCompound entryNBT = mobSpawnerEntry.entityNBT();
		Optional<EntityType<?>> entityType = EntityType.fromNbt(entryNBT);
		if(entityType.isEmpty())
			return;
		
		// Prevent non-Peaceful mobs from spawning in Peaceful difficulty
		if(world.getDifficulty() == Difficulty.PEACEFUL && !entityType.get().getSpawnGroup().isPeaceful())
			return;
		
		// Attempt to find a spawnable position
		Optional<BlockPos> spawnPos = findSpawnablePosition(mobSpawnerEntry, entityType.get(), spawnerPos, spawnArea, this::getSpawnRange, world, random);
		if(spawnPos.isEmpty())
			return;
		
		// Generate the entity to spawn
		BlockPos blockPos = spawnPos.get();
		Entity entity = EntityType.loadEntityWithPassengers(entryNBT, world, SpawnReason.SPAWNER, e -> 
		{
			e.refreshPositionAndAngles(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, e.getYaw(), e.getPitch());
			return e;
		});
		if(entity == null)
			return;
		
		entity.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0F, 0.0F);
		
		// Populate mob equipment
		if(entity instanceof MobEntity mobEntity)
		{
			if(mobSpawnerEntry.customSpawnRules().isEmpty() && !mobEntity.canSpawn(world, SpawnReason.SPAWNER) || !mobEntity.canSpawn(world))
				return;
			
			// If no complex entity data present, equip according to local difficulty
			if(mobSpawnerEntry.entityNBT().getSize() == 1 && mobSpawnerEntry.entityNBT().contains("id", 8))
				((MobEntity)entity).initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.SPAWNER, null);
			
			mobSpawnerEntry.equipment().ifPresent(mobEntity::setEquipmentFromTable);
		}
		
		// Actually spawn the entity
		if(!world.spawnNewEntityAndPassengers(entity))
			return;
		
		world.syncWorldEvent(2004, spawnerPos, 0);
		world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
		if(entity instanceof MobEntity)
			((MobEntity)entity).playSpawnEffects();
		
		return;
	}
	
	protected static Optional<BlockPos> findSpawnablePosition(
			SpawnerEntry mobSpawnerEntry, 
			EntityType<?> entityType, 
			BlockPos spawnerPos, 
			Box spawnArea, 
			Function<Axis,Integer> rangeGetter, 
			ServerWorld world, 
			Random random)
	{
		/** Set of any predefined coordinates */
		final NbtList coordinates = mobSpawnerEntry.entityNBT().getList("Pos", NbtElement.DOUBLE_TYPE);
		
		final Optional<MobSpawnerEntry.CustomSpawnRules> customSpawnRules = mobSpawnerEntry.customSpawnRules();
		/** Predicate defining any spawn rules for this entry */
		final Predicate<BlockPos> spawnRules = blockPos -> 
		{
			if(customSpawnRules.isPresent() && !customSpawnRules.get().canSpawn(blockPos, world))
				return false;
			
			if(!SpawnRestriction.canSpawn(entityType, world, SpawnReason.SPAWNER, blockPos, random))
				return false;
			
			return true;
		};
		
		int attempts = 10;
		do
		{
			// Retrieve or calculate positions within range of the spawner
			int posX = getOrMakeCoordinate(Axis.X, coordinates, spawnerPos, spawnArea, rangeGetter, random);
			int posY = getOrMakeCoordinate(Axis.Y, coordinates, spawnerPos, spawnArea, rangeGetter, random);
			int posZ = getOrMakeCoordinate(Axis.Z, coordinates, spawnerPos, spawnArea, rangeGetter, random);
			
			// Check if we could spawn the mob at this position
			final BlockPos pos = new BlockPos(posX, posY, posZ);
			if(world.isSpaceEmpty(entityType.getSpawnBox(posX + 0.5D, posY, posZ + 0.5D)) && spawnRules.test(pos))
				return Optional.of(pos);
		}
		while(attempts-- > 0);
		
		// If we failed to find a spawn position, try to use the spawner's position directly
		return spawnRules.test(spawnerPos) ? Optional.of(spawnerPos) : Optional.empty();
	}
	
	protected static int getOrMakeCoordinate(Axis axis, NbtList coordinates, BlockPos spawnerPos, Box spawnArea, Function<Axis, Integer> rangeGetter, Random random)
	{
		// If this coordinate has been predefined, retrieve it 
		if(axis.ordinal() < coordinates.size())
			return (int)Math.floor(coordinates.getDouble(axis.ordinal()));
		
		// The "origin" value of this axis based on the spawner's position
		int origin = axis.choose(spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ());
		
		// How far we can move on this axis
		int range = rangeGetter.apply(axis);
		if(range > 0)
		{
			int min = (int)axis.choose(spawnArea.minX, spawnArea.minY, spawnArea.minZ) * range;
			int max = (int)axis.choose(spawnArea.maxX, spawnArea.maxY, spawnArea.maxZ) * range;
			
			// Calculate the position along this axis within our range
			int local = min + (int)((max - min) * random.nextDouble());
			
			// Combine the local position with the origin
			return origin + local;
		}
		else
			return origin;
	}
	
	/** Sets a random option from the spawn potentials list as the current spawn entry */
	protected void updateSpawns(World world, BlockPos pos, Random random)
	{
		this.spawnPotentials.getOrEmpty(random).ifPresent(spawnPotential -> this.setSpawnEntry((SpawnerEntry)spawnPotential.data()));
		world.addSyncedBlockEvent(pos, Blocks.SPAWNER, 1, 0);
	}
	
	public record SpawnerEntry(NbtCompound entityNBT, Optional<MobSpawnerEntry.CustomSpawnRules> customSpawnRules, Optional<EquipmentTable> equipment, int min, int max)
	{
		public static final Codec<SpawnerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			NbtCompound.CODEC.fieldOf("entity").forGetter(SpawnerEntry::entityNBT),
			MobSpawnerEntry.CustomSpawnRules.CODEC.optionalFieldOf("custom_spawn_rules").forGetter(SpawnerEntry::customSpawnRules),
			EquipmentTable.CODEC.optionalFieldOf("equipment").forGetter(SpawnerEntry::equipment),
			Codec.INT.fieldOf("min").forGetter(SpawnerEntry::min),
			Codec.INT.fieldOf("max").forGetter(SpawnerEntry::max))
			.apply(instance, SpawnerEntry::new));
		public static final Codec<DataPool<SpawnerEntry>> DATA_POOL_CODEC = DataPool.createEmptyAllowedCodec(CODEC);
		
		public SpawnerEntry()
		{
			this(new NbtCompound(), Optional.empty(), Optional.empty(), 1, 1);
		}
		
		public SpawnerEntry(NbtCompound entityNBT, Optional<MobSpawnerEntry.CustomSpawnRules> customSpawnRules, Optional<EquipmentTable> equipment, int min, int max)
		{
			if(entityNBT.contains("id"))
			{
				Identifier identifier = Identifier.tryParse(entityNBT.getString("id"));
				if(identifier != null)
					entityNBT.putString("id", identifier.toString());
				else
					entityNBT.remove("id");
			}
			
			this.entityNBT = entityNBT;
			this.customSpawnRules = customSpawnRules;
			this.equipment = equipment;
			this.min = Math.max(min, 1);
			this.max = Math.max(max, 1);
		}
		
		public int countToSpawn(Random random)
		{
			return random.nextBetween(min, max);
		}
		
		public static class Builder
		{
			private final EntityType<?> type;
			private NbtCompound entityNbt = new NbtCompound();
			private Optional<MobSpawnerEntry.CustomSpawnRules> spawnRules = Optional.empty();
			private Optional<EquipmentTable> equipment = Optional.empty();
			private int min = 1, max = 1;
			
			private Builder(EntityType<?> typeIn)
			{
				type = typeIn;
			}
			
			public static Builder of(EntityType<?> typeIn) { return new Builder(typeIn); }
			
			public Builder count(int val)
			{
				min = max = val;
				return this;
			}
			
			public Builder count(int min, int max)
			{
				this.min = Math.min(min, max);
				this.max = Math.max(min, max);
				return this;
			}
			
			public Builder nbt(NbtCompound nbt)
			{
				entityNbt = nbt;
				return this;
			}
			
			public Builder spawnRules(MobSpawnerEntry.CustomSpawnRules spawnRulesIn)
			{
				spawnRules = Optional.of(spawnRulesIn);
				return this;
			}
			
			public Builder equipment(EquipmentTable table)
			{
				equipment = Optional.of(table);
				return this;
			}
			
			public SpawnerEntry build()
			{
				entityNbt.putString("id", EntityType.getId(type).toString());
				return new SpawnerEntry(entityNbt, spawnRules, equipment, min, max);
			}
		}
	}
}