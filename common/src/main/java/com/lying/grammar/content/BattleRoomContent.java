package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.grammar.content.BattleRoomContent.BattleEntry;
import com.lying.grammar.content.battle.CrowdBattleEntry;
import com.lying.grammar.content.battle.SquadBattleEntry;
import com.lying.worldgen.theme.Theme;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public class BattleRoomContent extends RegistryRoomContent<BattleEntry>
{
	public static final Identifier ID	= prefix("combat_encounter");
	private static final Map<Identifier, Supplier<? extends BattleEntry>> REGISTRY	= new HashMap<>();
	
	public static final Supplier<CrowdBattleEntry<?>> CROWD	= registerBattle("crowd", CrowdBattleEntry::new);
	public static final Supplier<SquadBattleEntry> BASIC_SQUAD	= registerBattle("basic_squad", SquadBattleEntry::new);
	
	public BattleRoomContent()
	{
		super(ID);
	}
	
	private static <T extends BattleEntry> Supplier<T> registerBattle(String nameIn, Function<Identifier, T> factory)
	{
		return registerBattle(prefix(nameIn), factory);
	}
	
	public static <T extends BattleEntry> Supplier<T> registerBattle(Identifier idIn, Function<Identifier, T> factory)
	{
		Supplier<T> supplier = () -> factory.apply(idIn);
		REGISTRY.put(idIn, supplier);
		return supplier;
	}
	
	public static Supplier<? extends BattleEntry> getBattle(Identifier idIn)
	{
		return REGISTRY.getOrDefault(idIn, CROWD);
	}
	
	public void buildRegistry(Theme theme)
	{
		theme.encounters().forEach(encounter -> register(encounter.name(), encounter));
	}
	
	public static class EncounterSet extends ArrayList<BattleEntry>
	{
		private static final long serialVersionUID = 1L;
		public static final Codec<EncounterSet> CODEC	= BattleEntry.CODEC.listOf().comapFlatMap(
				list -> DataResult.success(new EncounterSet(list)),
				set -> new ArrayList<BattleEntry>(set)
				);
		
		public EncounterSet() { }
		
		public EncounterSet(List<BattleEntry> entriesIn)
		{
			this();
			entriesIn.forEach(this::addEntry);
		}
		
		public JsonElement toJson(JsonOps ops)
		{
			return CODEC.encodeStart(ops, this).getOrThrow();
		}
		
		public static EncounterSet fromJson(JsonOps ops, JsonElement ele)
		{
			return CODEC.parse(ops, ele).getOrThrow();
		}
		
		public EncounterSet addEntry(@Nullable BattleEntry entry)
		{
			if(entry != null)
				add(entry);
			return this;
		}
		
		public EncounterSet addSquad(Identifier name, SquadBattleEntry squad)
		{
			return addEntry(squad.setName(name));
		}
		
		public <T extends MobEntity> EncounterSet addCrowd(Identifier name, EntityType<T> typeIn, int count)
		{
			return addCrowd(name, typeIn, count, count);
		}
		
		public <T extends MobEntity> EncounterSet addCrowd(Identifier name, EntityType<T> typeIn, int min, int max)
		{
			return addEntry(BattleRoomContent.CROWD.get().of(typeIn, min, max).setName(name));
		}
	}
	
	public static abstract class BattleEntry implements IContentEntry
	{
		public static final Codec<BattleEntry> CODEC = Codec.of(BattleEntry::encode, BattleEntry::decode);
		private static final BattleEntry ERROR	= BattleRoomContent.CROWD.get().of(EntityType.RABBIT, 1).setName(prefix("error"));
		protected static final int SEARCH_ATTEMPTS = 20;
		protected final Identifier registryName;
		protected Identifier name;
		
		protected BattleEntry(Identifier idIn)
		{
			registryName = idIn;
		}
		
		public Identifier registryName() { return registryName; }
		
		public Identifier name() { return name; }
		
		public final BattleEntry setName(Identifier nameIn)
		{
			name = nameIn;
			return this;
		}
		
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
		
		public JsonObject toJson(JsonOps ops)
		{
			JsonObject obj = new JsonObject();
			obj.addProperty("id", registryName.toString());
			obj.addProperty("name", name.toString());
			writeToJson(ops, obj);
			return obj;
		}
		
		protected abstract void writeToJson(JsonOps ops, JsonObject obj);
		
		@NotNull
		public static BattleEntry fromJson(JsonOps ops, JsonObject obj)
		{
			Identifier id = Identifier.of(obj.get("id").getAsString());
			Supplier<? extends BattleEntry> type = BattleRoomContent.getBattle(id);
			if(type == null)
			{
				CyclicDungeons.LOGGER.error(" # Unrecognised battle entry type {}, replaced with a rabbit", id.toString());
				return ERROR;
			}
			
			Identifier name = Identifier.of(obj.get("name").getAsString());
			BattleEntry result = type.get().setName(name).readFromJson(ops, obj);
			if(result == null)
			{
				CyclicDungeons.LOGGER.error(" # Error whilst reading battle entry {} of type {}, replaced with a rabbit", name.toString(), id.toString());
				return ERROR;
			}
			else
				return result;
		}
		
		@Nullable
		protected abstract BattleEntry readFromJson(JsonOps ops, JsonObject obj);
		
		@SuppressWarnings("unchecked")
		private static <T> DataResult<T> encode(final BattleEntry func, final DynamicOps<T> ops, final T prefix)
		{
			return ops == JsonOps.INSTANCE ? (DataResult<T>)DataResult.success(func.toJson(JsonOps.INSTANCE)) : DataResult.error(() -> "Storing battle entry as NBT is not supported");
		}
		
		private static <T> DataResult<Pair<BattleEntry, T>> decode(final DynamicOps<T> ops, final T input)
		{
			return ops == JsonOps.INSTANCE ? DataResult.success(Pair.of(fromJson(JsonOps.INSTANCE, (JsonObject)input), input)) : DataResult.error(() -> "Loading battle entry from NBT is not supported");
		}
	}
}
