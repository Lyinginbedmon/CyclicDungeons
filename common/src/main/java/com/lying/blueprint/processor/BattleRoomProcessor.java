package com.lying.blueprint.processor;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.blueprint.processor.BattleRoomProcessor.BattleEntry;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDThemes.Theme;
import com.lying.reference.Reference;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public class BattleRoomProcessor extends RegistryRoomProcessor<BattleEntry>
{
	public void buildRegistry(Theme theme)
	{
		theme.encounters().forEach(encounter -> register(encounter.registryName(), encounter));
	}
	
	public static abstract class BattleEntry implements IProcessorEntry
	{
		protected static final int SEARCH_ATTEMPTS = 20;
		private final Identifier id;
		
		private BattleEntry(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
		
		@Nullable
		protected static BlockPos findSpawnablePosition(EntityType<? extends Entity> type, BlockPos min, BlockPos max, ServerWorld world, Random rand, int searchAttempts)
		{
			BlockPos pos;
			boolean isValid = false;
			do
			{
				pos = new BlockPos(
					rand.nextBetween(min.getX() + 1, max.getX() - 1), 
					rand.nextBetween(min.getY() + 1, max.getY() - 1), 
					rand.nextBetween(min.getZ() + 1, max.getZ() - 1));
				isValid = canPlaceAt(pos, world, type);
			}
			while(--searchAttempts > 0 && !isValid);
			return isValid ? pos : null;
		}
		
		protected static <T extends Entity> boolean canPlaceAt(BlockPos pos, ServerWorld world, EntityType<T> type)
		{
			if(!SpawnLocationTypes.ON_GROUND.isSpawnPositionOk(world, pos, type))
				return false;
			
			if(!Heightmap.Type.MOTION_BLOCKING_NO_LEAVES.getBlockPredicate().test(world.getBlockState(pos.down())))
				return false;
			
			if(!world.isSpaceEmpty(type.getSpawnBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)))
				return false;
			
			Entity entity = type.create(world, SpawnReason.STRUCTURE);
			entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0, 0);
			
			if(entity instanceof MobEntity)
			{
				MobEntity mob = (MobEntity)entity;
				if(!mob.canSpawn(world))
					return false;
				
				if(!mob.canSpawn(world, SpawnReason.STRUCTURE))
					return false;
			}
			
			return true;
		}
		
		protected static <T extends Entity> boolean trySpawn(EntityType<T> type, BlockPos pos, ServerWorld world)
		{
			Entity entity = type.create(world, SpawnReason.STRUCTURE);
			entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 360F * world.random.nextFloat(), 0);
			if(entity instanceof MobEntity)
			{
				MobEntity mob = (MobEntity)entity;
				mob.setPersistent();
				mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
				
				// Where possible, set the mob's home position to encourage them to stay in the room
				if(mob.getBrain().hasMemoryModule(MemoryModuleType.HOME))
					mob.getBrain().remember(MemoryModuleType.HOME, new GlobalPos(world.getRegistryKey(), pos));
				
				if(world.spawnNewEntityAndPassengers(entity))
					return true;
			}
			return false;
		}
	}
	
	/** Spawns a group of all the same mob */
	public static class SimpleBattleEntry<T extends MobEntity> extends BattleEntry
	{
		private final EntityType<T> type;
		private final int maxCount, minCount;
		
		public SimpleBattleEntry(Identifier name, EntityType<T> typeIn, int min, int max)
		{
			super(name);
			type = typeIn;
			minCount = min;
			maxCount = max;
		}
		
		public SimpleBattleEntry(Identifier name, EntityType<T> typeIn, int count)
		{
			this(name, typeIn, count, count);
		}
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
		{
			Random rand = world.random;
			final int mobs = rand.nextBetween(minCount, maxCount);
			for(int i=mobs; i>0; --i)
			{
				BlockPos pos = findSpawnablePosition(type, min, max, world, rand, SEARCH_ATTEMPTS);
				if(pos != null)
					trySpawn(type, pos, world);
			}
		}
	}
	
	/** Spawns a predefined set of mobs */
	public static class SquadBattleEntry extends BattleEntry
	{
		private final List<SquadEntry> squad = Lists.newArrayList();
		private Consumer<Roster> setup = Consumers.nop();
		
		public SquadBattleEntry(Identifier name)
		{
			super(name);
		}
		
		public SquadBattleEntry add(SquadEntry entry)
		{
			squad.add(entry);
			return this;
		}
		
		public SquadBattleEntry setup(Consumer<Roster> setupIn)
		{
			setup = setupIn;
			return this;
		}
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
		{
			Random rand = world.random;
			Roster roster = new Roster();
			for(SquadEntry entry : squad)
			{
				List<MobEntity> list = roster.getOrDefault(entry.name(), Lists.newArrayList());
				int count = 
						entry.min() == entry.max() ? 
							entry.min() : 
							world.random.nextBetween(entry.min(), entry.max());
				
				if(count > 0)
					for(int i=0; i<count; i++)
					{
						BlockPos pos = findSpawnablePosition(entry.type, min, max, world, rand, SEARCH_ATTEMPTS);
						if(pos != null)
						{
							MobEntity mob = entry.trySpawn(pos, world);
							if(mob != null)
								list.add(mob);
						}
					}
				roster.put(entry.name(), list);
			}
			setup.accept(roster);
		}
		
		/** Delineated set of mobs representing different roles in the squad */
		public static class Roster extends HashMap<Identifier, List<MobEntity>>
		{
			private static final long serialVersionUID = 7601896549322837235L;
			public static final Identifier DEFAULT_NAME	= prefix("mob");
			
			public List<MobEntity> get(String nameIn)
			{
				return get(prefix(nameIn));
			}
			
			public List<MobEntity> getOrDefault(String nameIn, List<MobEntity> defaultValue)
			{
				return getOrDefault(prefix(nameIn), defaultValue);
			}
		}
		
		public static record SquadEntry(EntityType<? extends MobEntity> type, Optional<NbtCompound> data, int min, int max, Identifier name)
		{
			protected MobEntity trySpawn(BlockPos pos, ServerWorld world)
			{
				Entity entity = type.create(world, SpawnReason.STRUCTURE);
				entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 360F * world.random.nextFloat(), 0);
				data.ifPresent(d -> entity.readNbt(d));
				
				if(entity instanceof MobEntity)
				{
					MobEntity mob = (MobEntity)entity;
					mob.setPersistent();
					mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
					
					// Where possible, set the mob's home position to encourage them to stay in the room
					if(mob.getBrain().hasMemoryModule(MemoryModuleType.HOME))
						mob.getBrain().remember(MemoryModuleType.HOME, new GlobalPos(world.getRegistryKey(), pos));
					
					if(world.spawnNewEntityAndPassengers(entity))
						return mob;
				}
				return null;
			}
			
			public static class Builder
			{
				private final EntityType<? extends MobEntity> type;
				private Optional<NbtCompound> data = Optional.empty();
				private Identifier name = Roster.DEFAULT_NAME;
				private int min = 1, max = 1;
				
				protected Builder(EntityType<? extends MobEntity> typeIn)
				{
					type = typeIn;
				}
				
				public static Builder of(EntityType<? extends MobEntity> typeIn)
				{
					return new Builder(typeIn);
				}
				
				public Builder name(Identifier nameIn)
				{
					name = nameIn;
					return this;
				}
				
				public Builder name(String nameIn)
				{
					return name(Reference.ModInfo.prefix(nameIn));
				}
				
				public Builder nbt(NbtCompound dataIn)
				{
					data = dataIn.isEmpty() ? Optional.empty() : Optional.of(dataIn);
					return this;
				}
				
				public Builder count(int val)
				{
					min = max = val;
					return this;
				}
				
				public Builder count(int a, int b)
				{
					min = Math.min(a, b);
					max = Math.max(a, b);
					return this;
				}
				
				public SquadEntry build()
				{
					return new SquadEntry(type, data, min, max, name);
				}
			}
		}
	}
}
