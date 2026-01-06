package com.lying.grammar.content.battle;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.BattleRoomContent.BattleEntry;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
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
	
	public SquadBattleEntry add(@Nullable SquadBattleEntry.SquadEntry entry)
	{
		if(entry != null)
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
			List<Entity> list = roster.getOrDefault(entry.name(), Lists.newArrayList());
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
						Entity mob = entry.trySpawn(pos, world);
						if(mob != null)
							list.add(mob);
					}
				}
			roster.put(entry.name(), list);
		}
		setup.accept(roster);
	}
	
	protected void writeToJson(JsonOps ops, JsonObject obj)
	{
		JsonArray set = new JsonArray();
		squad.forEach(s -> set.add(s.toJson(ops)));
		obj.add("squads", set);
	}
	
	protected BattleEntry readFromJson(JsonOps ops, JsonObject obj)
	{
		SquadBattleEntry entry = new SquadBattleEntry(name());
		
		JsonArray set = obj.getAsJsonArray("squads");
		set.forEach(ele -> 
		{
			SquadEntry e = SquadEntry.fromJson(ops, ele);
			if(e != null)
				entry.add(e);
		});
		
		return entry;
	}
	
	/** Delineated set of mobs representing different roles in the squad */
	public static class Roster extends HashMap<Identifier, List<Entity>>
	{
		private static final long serialVersionUID = 7601896549322837235L;
		public static final Identifier DEFAULT_NAME	= prefix("mob");
		
		public List<Entity> get(String nameIn)
		{
			return get(prefix(nameIn));
		}
		
		public List<Entity> getOrDefault(String nameIn, List<Entity> defaultValue)
		{
			return getOrDefault(prefix(nameIn), defaultValue);
		}
	}
	
	public static record SquadEntry(EntityType<? extends Entity> type, Optional<NbtCompound> data, int min, int max, Identifier name)
	{
		public static final Codec<SquadEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("type").forGetter(s -> s.type().arch$registryName()),
				NbtCompound.CODEC.optionalFieldOf("nbt").forGetter(SquadEntry::data),
				Codec.INT.fieldOf("min").forGetter(SquadEntry::min),
				Codec.INT.fieldOf("max").forGetter(SquadEntry::max),
				Identifier.CODEC.fieldOf("name").forGetter(SquadEntry::name)
				).apply(instance, (typeId, nbt, min, max, name) -> 
				{
					EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
					if(type == null)
						return null;
					
					SquadEntry.Builder builder = SquadEntry.Builder.of(type);
					builder.name(name);
					builder.count(min, max);
					nbt.ifPresent(builder::nbt);
					return builder.build();
				}));
		
		@Nullable
		protected Entity trySpawn(BlockPos pos, ServerWorld world)
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
			else if(world.spawnNewEntityAndPassengers(entity))
				return entity;
			
			return null;
		}
		
		public JsonElement toJson(JsonOps ops)
		{
			return CODEC.encodeStart(ops, this).getOrThrow();
		}
		
		@Nullable
		public static SquadEntry fromJson(JsonOps ops, JsonElement ele)
		{
			return CODEC.parse(ops, ele).getOrThrow();
		}
		
		public static class Builder
		{
			private final EntityType<? extends Entity> type;
			private Optional<NbtCompound> data = Optional.empty();
			private Identifier name = Roster.DEFAULT_NAME;
			private int min = 1, max = 1;
			
			protected Builder(EntityType<? extends Entity> typeIn)
			{
				type = typeIn;
			}
			
			public static SquadEntry.Builder of(EntityType<? extends Entity> typeIn)
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