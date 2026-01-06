package com.lying.grammar.content.battle;

import com.google.gson.JsonObject;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.BattleRoomContent.BattleEntry;
import com.mojang.serialization.JsonOps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Spawns a group of all the same mob */
public class CrowdBattleEntry<T extends Entity> extends BattleEntry
{
	private final EntityType<T> type;
	private final int maxCount, minCount;
	
	protected CrowdBattleEntry(Identifier registryNameIn, EntityType<T> typeIn, int min, int max)
	{
		super(registryNameIn);
		type = typeIn;
		minCount = min;
		maxCount = max;
	}
	
	public CrowdBattleEntry(Identifier registryNameIn)
	{
		this(registryNameIn, null, 1, 1);
	}
	
	public <J extends Entity> CrowdBattleEntry<J> of(EntityType<J> typeIn)
	{
		return of(typeIn, 1);
	}
	
	public <J extends Entity> CrowdBattleEntry<J> of(EntityType<J> typeIn, int count)
	{
		return of(typeIn, count, count);
	}
	
	public <J extends Entity> CrowdBattleEntry<J> of(EntityType<J> typeIn, int min, int max)
	{
		return new CrowdBattleEntry<>(registryName(), typeIn, min, max);
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
	
	protected void writeToJson(JsonOps ops, JsonObject obj)
	{
		obj.addProperty("type", type.arch$registryName().toString());
		if(minCount != maxCount)
		{
			obj.addProperty("min", minCount);
			obj.addProperty("max", maxCount);
		}
		else
			obj.addProperty("count", minCount);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected BattleEntry readFromJson(JsonOps ops, JsonObject obj)
	{
		if(!obj.has("type"))
			return null;
		
		EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(obj.get("type").getAsString()));
		if(type == null)
			return null;
		
		int min, max;
		if(obj.has("count"))
			min = max = obj.get("count").getAsInt();
		else if(obj.has("min") && obj.has("max"))
		{
			min = obj.get("min").getAsInt();
			max = obj.get("max").getAsInt();
		}
		else
			min = max = 1;
		
		return new CrowdBattleEntry(name(), type, min, max);
	}
}