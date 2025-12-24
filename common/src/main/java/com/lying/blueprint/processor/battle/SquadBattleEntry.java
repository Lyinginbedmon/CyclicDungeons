package com.lying.blueprint.processor.battle;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.function.Consumers;

import com.google.common.collect.Lists;
import com.lying.blueprint.processor.BattleRoomProcessor.BattleEntry;
import com.lying.grammar.RoomMetadata;
import com.lying.reference.Reference;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;

/** Spawns a predefined set of mobs */
public class SquadBattleEntry extends BattleEntry
{
	private final List<SquadBattleEntry.SquadEntry> squad = Lists.newArrayList();
	private Consumer<SquadBattleEntry.Roster> setup = Consumers.nop();
	
	public SquadBattleEntry(Identifier name)
	{
		super(name);
	}
	
	public SquadBattleEntry add(SquadBattleEntry.SquadEntry entry)
	{
		squad.add(entry);
		return this;
	}
	
	public SquadBattleEntry setup(Consumer<SquadBattleEntry.Roster> setupIn)
	{
		setup = setupIn;
		return this;
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		Random rand = world.random;
		SquadBattleEntry.Roster roster = new Roster();
		for(SquadBattleEntry.SquadEntry entry : squad)
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
			
			public static SquadEntry.Builder of(EntityType<? extends MobEntity> typeIn)
			{
				return new Builder(typeIn);
			}
			
			public SquadEntry.Builder name(Identifier nameIn)
			{
				name = nameIn;
				return this;
			}
			
			public SquadEntry.Builder name(String nameIn)
			{
				return name(Reference.ModInfo.prefix(nameIn));
			}
			
			public SquadEntry.Builder nbt(NbtCompound dataIn)
			{
				data = dataIn.isEmpty() ? Optional.empty() : Optional.of(dataIn);
				return this;
			}
			
			public SquadEntry.Builder count(int val)
			{
				min = max = val;
				return this;
			}
			
			public SquadEntry.Builder count(int a, int b)
			{
				min = Math.min(a, b);
				max = Math.max(a, b);
				return this;
			}
			
			public SquadBattleEntry.SquadEntry build()
			{
				return new SquadEntry(type, data, min, max, name);
			}
		}
	}
}