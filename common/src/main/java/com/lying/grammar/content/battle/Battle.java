package com.lying.grammar.content.battle;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDBattleTypes;
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

public abstract class Battle
{
	public static final Codec<Battle> CODEC = Codec.of(Battle::encode, Battle::decode);
	protected static final int SEARCH_ATTEMPTS = 20;
	protected final Identifier typeName;
	
	protected Battle(Identifier idIn)
	{
		typeName = idIn;
	}
	
	public Identifier registryName() { return typeName; }
	
	public abstract void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta);
	
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
		obj.addProperty("battle_type", typeName.toString());
		writeToJson(ops, obj);
		return obj;
	}
	
	protected abstract void writeToJson(JsonOps ops, JsonObject obj);
	
	@Nullable
	public static Battle fromJson(JsonOps ops, JsonObject obj)
	{
		Identifier id = Identifier.of(obj.get("battle_type").getAsString());
		Battle type = CDBattleTypes.get(id).orElse(null);
		if(type == null)
		{
			CyclicDungeons.LOGGER.error(" # Unrecognised battle entry type {}", id.toString());
			return null;
		}
		
		Battle result = type.readFromJson(ops, obj);
		if(result == null)
		{
			CyclicDungeons.LOGGER.error(" # Error whilst reading battle of type {}", id.toString());
			return null;
		}
		else
			return result;
	}
	
	@Nullable
	protected abstract Battle readFromJson(JsonOps ops, JsonObject obj);
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final Battle func, final DynamicOps<T> ops, final T prefix)
	{
		return ops == JsonOps.INSTANCE ? (DataResult<T>)DataResult.success(func.toJson(JsonOps.INSTANCE)) : DataResult.error(() -> "Storing battle as NBT is not supported");
	}
	
	private static <T> DataResult<Pair<Battle, T>> decode(final DynamicOps<T> ops, final T input)
	{
		return ops == JsonOps.INSTANCE ? DataResult.success(Pair.of(fromJson(JsonOps.INSTANCE, (JsonObject)input), input)) : DataResult.error(() -> "Loading battle from NBT is not supported");
	}
}