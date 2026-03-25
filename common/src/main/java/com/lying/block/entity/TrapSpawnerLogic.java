package com.lying.block.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.init.CDBlocks;
import com.lying.init.CDSoundEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentTable;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
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
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class TrapSpawnerLogic
{
	@Nullable
	public SpawnerEntry spawnEntry;
	protected DataPool<SpawnerEntry> spawnPotentials = DataPool.<SpawnerEntry>empty();
	protected int[] spawnRanges = new int[] {4, 4, 4};
	protected BlockPos fallbackOffset = BlockPos.ORIGIN;
	protected Optional<BlockPos> forcePos = Optional.empty();
	
	public TrapSpawnerLogic() { }
	
	public TrapSpawnerLogic(SpawnerEntry entry)
	{
		this();
		spawnEntry = entry;
	}
	
	public TrapSpawnerLogic(List<SpawnerEntry> entries)
	{
		this();
		if(!entries.isEmpty())
			if(entries.size() > 1)
			{
				setSpawnPotentials(entries);
				spawnEntry = null;
			}
			else
				spawnEntry = entries.get(0);
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
			this.spawnRanges = nbt.getIntArray("SpawnRange");
		
		if(nbt.contains("Fallback"))
			this.fallbackOffset = NbtHelper.toBlockPos(nbt, "Fallback").orElse(BlockPos.ORIGIN);
		
		this.forcePos = nbt.contains("ForcedPos") ? NbtHelper.toBlockPos(nbt, "ForcedPos") : Optional.empty();
	}
	
	public NbtCompound writeNbt(NbtCompound nbt)
	{
		nbt.putIntArray("SpawnRange", spawnRanges);
		if(fallbackOffset.getManhattanDistance(BlockPos.ORIGIN) > 0)
			nbt.put("Fallback", NbtHelper.fromBlockPos(fallbackOffset));
		forcePos.ifPresent(p -> nbt.put("ForcedPos", NbtHelper.fromBlockPos(p)));
		if(this.spawnEntry != null)
			nbt.put("SpawnData", SpawnerEntry.CODEC.encodeStart(NbtOps.INSTANCE, spawnEntry).getOrThrow(s -> new IllegalStateException("Invalid SpawnData: " + s)));
		if(!spawnPotentials.isEmpty())
			nbt.put("SpawnPotentials", SpawnerEntry.DATA_POOL_CODEC.encodeStart(NbtOps.INSTANCE, spawnPotentials).getOrThrow());
		return nbt;
	}
	
	public void clientTick(World world, BlockPos pos)
	{
		Random random = world.getRandom();
		if(random.nextInt(8) > 0)
			return;
		
		double d = (double)pos.getX() + random.nextDouble();
		double e = (double)pos.getY() + random.nextDouble();
		double f = (double)pos.getZ() + random.nextDouble();
		world.addParticle(ParticleTypes.WITCH, d, e, f, 0.0, 0.0, 0.0);
	}
	
	public TrapSpawnerLogic setEntityId(EntityType<?> type, @Nullable World world, Random random, BlockPos pos)
	{
		getSpawnEntry(world, random, pos).entityNBT().putString("id", Registries.ENTITY_TYPE.getId(type).toString());
		return this;
	}
	
	public TrapSpawnerLogic setByEntityType(EntityType<?> type)
	{
		SpawnerEntry entry = SpawnerEntry.Builder.of(type).build();
		this.spawnEntry = entry;
		setSpawnPotentials(List.of(entry));
		return this;
	}
	
	public TrapSpawnerLogic setByEntity(Entity entity)
	{
		NbtCompound nbt = new NbtCompound();
		entity.saveSelfNbt(nbt);
		nbt.remove("Pos");
		nbt.remove("Motion");
		
		SpawnerEntry entry = SpawnerEntry.Builder.of(entity.getType()).nbt(nbt).build();
		this.spawnEntry = entry;
		setSpawnPotentials(List.of(entry));
		return this;
	}
	
	public TrapSpawnerLogic setSpawnPotentials(List<SpawnerEntry> entries)
	{
		DataPool.Builder<SpawnerEntry> builder = DataPool.builder();
		entries.forEach(builder::add);
		this.spawnPotentials = builder.build();
		return this;
	}
	
	public void setSpawnPotentials(NbtElement list)
	{
		this.spawnPotentials = SpawnerEntry.DATA_POOL_CODEC
				.parse(NbtOps.INSTANCE, list)
				.resultOrPartial(e -> CyclicDungeons.LOGGER.warn("Invalid SpawnPotentials list: {}", e))
				.orElseGet(() -> DataPool.<SpawnerEntry>empty());
	}
	
	public int getSpawnRange(Axis index) { return index.ordinal() < spawnRanges.length ? spawnRanges[index.ordinal()] : 0; }
	
	public TrapSpawnerLogic setSpawnRange(Vector3i vec)
	{
		return setSpawnRange(vec.x, vec.y, vec.z);
	}
	
	public TrapSpawnerLogic setSpawnRange(int x, int y, int z)
	{
		this.spawnRanges = new int[] {x, y, z};
		return this;
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
	
	public TrapSpawnerLogic setSpawnEntry(SpawnerEntry spawnEntry)
	{
		this.spawnEntry = spawnEntry;
		return this;
	}
	
	protected void spawnNextEntry(ServerWorld world, BlockPos spawnerPos, Box spawnArea)
	{
		final Random random = world.getRandom();
		SpawnerEntry entry = this.getSpawnEntry(world, random, spawnerPos);
		processEntry(entry, world, spawnerPos, this.forcePos, this.fallbackOffset, spawnArea, random);
		this.updateSpawns(world, spawnerPos, random);
	}
	
	private static void processEntry(SpawnerEntry entry, ServerWorld world, BlockPos spawnerPos, Optional<BlockPos> forcePos, BlockPos fallbackOffset, Box spawnArea, Random random)
	{
		Optional<EntityType<?>> type = EntityType.fromNbt(entry.entityNBT());
		if(type == null || type.isEmpty())
			return;
		else if(world.getDifficulty() == Difficulty.PEACEFUL && !type.get().getSpawnGroup().isPeaceful())
			return;
		
		// Build map of spawnable positions within spawn area
		List<BlockPos> positions = Lists.newArrayList();
		forcePos.ifPresentOrElse(p -> positions.add(spawnerPos.add(p)), () -> 
		{
			// Scan spawn area for useable positions
			BlockPos.Mutable.iterate(
					spawnerPos.add((int)spawnArea.minX, (int)spawnArea.minY, (int)spawnArea.minZ), 
					spawnerPos.add((int)spawnArea.maxX, (int)spawnArea.maxY, (int)spawnArea.maxZ).subtract(new Vec3i(1,1,1)))
			.forEach(p -> 
			{
				if(isPositionSpawnable(p, world, type.get(), entry))
					positions.add(p.toImmutable());
			});
		});
		if(positions.isEmpty() && fallbackOffset.getManhattanDistance(BlockPos.ORIGIN) > 0)
		{
			BlockPos fallback = spawnerPos.add(fallbackOffset);
			if(isPositionSpawnable(fallback, world, type.get(), entry))
				positions.add(fallback);
		}
		
		// If map is not empty, do spawns
		if(!positions.isEmpty())
			for(int i = 0; i < entry.countToSpawn(world.getRandom()); i++)
				trySpawnEntry(entry, world, positions, random).ifPresent(positions::remove);
	}
	
	protected static Optional<BlockPos> trySpawnEntry(
			SpawnerEntry mobSpawnerEntry, 
			ServerWorld world, 
			List<BlockPos> viablePoints, 
			Random random)
	{
		NbtCompound entryNBT = mobSpawnerEntry.entityNBT();
		Optional<EntityType<?>> entityType = EntityType.fromNbt(entryNBT);
		if(entityType.isEmpty())
			return Optional.empty();
		
		// Prevent non-Peaceful mobs from spawning in Peaceful difficulty
		if(world.getDifficulty() == Difficulty.PEACEFUL && !entityType.get().getSpawnGroup().isPeaceful())
			return Optional.empty();
		
		// Generate the entity to spawn
		final BlockPos spawnPos = viablePoints.size() == 1 ? viablePoints.get(0) : viablePoints.get(random.nextInt(viablePoints.size()));
		Entity entity = EntityType.loadEntityWithPassengers(entryNBT, world, SpawnReason.SPAWNER, e -> 
		{
			e.refreshPositionAndAngles(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, e.getYaw(), e.getPitch());
			return e;
		});
		if(entity == null)
			return Optional.empty();
		
		entity.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0F, 0.0F);
		
		// Populate mob equipment
		if(entity instanceof MobEntity mobEntity)
		{
			// Check if we're going to cause entity overcrowding and avoid
			if(!mobEntity.canSpawn(world))
				return Optional.of(spawnPos);
			
			// If no complex entity data present, equip according to local difficulty
			if(mobSpawnerEntry.entityNBT().getSize() == 1 && mobSpawnerEntry.entityNBT().contains("id", 8))
				((MobEntity)entity).initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.SPAWNER, null);
			
			mobSpawnerEntry.equipment().ifPresent(mobEntity::setEquipmentFromTable);
		}
		
		// Actually spawn the entity
		if(!world.spawnNewEntityAndPassengers(entity))
			return Optional.empty();
		
		world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, spawnPos);
		if(entity instanceof MobEntity)
			((MobEntity)entity).playSpawnEffects();
		
		entity.playSound(CDSoundEvents.SPAWNER_SPAWN.get(), 0.5F, random.nextFloat() * 0.5F + 0.5F);
		return Optional.of(spawnPos);
	}
	
	protected static boolean isPositionSpawnable(BlockPos pos, ServerWorld world, EntityType<?> entityType, SpawnerEntry mobSpawnerEntry)
	{
		final Optional<MobSpawnerEntry.CustomSpawnRules> customSpawnRules = mobSpawnerEntry.customSpawnRules();
		/** Predicate defining any spawn rules for this entry */
		final Predicate<BlockPos> spawnRules = getSpawnConditions(customSpawnRules, entityType, world);
		return world.isSpaceEmpty(entityType.getSpawnBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)) && spawnRules.test(pos);
	}
	
	/**
	 * Returns a bespoke predicate determining if a position is valid for spawning the mob.<br>
	 * This bypasses other checks such as biome validity and light level, to ensure a safe but still flexible predicate.
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends Entity> Predicate<BlockPos> getSpawnConditions(Optional<MobSpawnerEntry.CustomSpawnRules> customSpawnRules, EntityType<T> entityType, ServerWorld world)
	{
		final T entity = entityType.create(world, SpawnReason.COMMAND);
		final Predicate<BlockPos> custom = p -> customSpawnRules.isEmpty() || customSpawnRules.get().canSpawn(p, world);
		Predicate<BlockPos> mob = p -> 
		{
			if(entity instanceof MobEntity)
			{
				EntityType<? extends MobEntity> mobType = (EntityType<? extends MobEntity>)entityType;
				
				// Does the blockstate permit spawning?
				if(!MobEntity.canMobSpawn(mobType, world, SpawnReason.SPAWN_ITEM_USE, p, world.getRandom()))
					return false;
				
				// Is the spawn location valid for this mob type?
				if(!SpawnRestriction.isSpawnPosAllowed(mobType, world, p))
					return false;
				
				// Is the position pathable?
				if(entity instanceof PathAwareEntity && !PathAwareEntity.canMobSpawn(mobType, world, SpawnReason.SPAWN_ITEM_USE, p, world.getRandom()))
					return false;
			}
			
			return true;
		};
		return blockPos -> custom.test(blockPos) && mob.test(blockPos);
	}
	
	/** Sets a random option from the spawn potentials list as the current spawn entry */
	protected void updateSpawns(World world, BlockPos pos, Random random)
	{
		this.spawnPotentials.getOrEmpty(random).ifPresent(spawnPotential -> this.setSpawnEntry((SpawnerEntry)spawnPotential.data()));
		world.addSyncedBlockEvent(pos, CDBlocks.SPAWNER.get(), 1, 0);
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
			
			public Builder nbt(Function<NbtCompound, NbtCompound> nbt)
			{
				return nbt(nbt.apply(new NbtCompound()));
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