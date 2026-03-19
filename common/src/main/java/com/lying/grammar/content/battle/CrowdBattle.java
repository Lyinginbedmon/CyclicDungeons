package com.lying.grammar.content.battle;

import com.google.gson.JsonObject;
import com.lying.grammar.RoomMetadata;
import com.lying.reference.Reference;
import com.mojang.serialization.JsonOps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Spawns a group of all the same mob */
public class CrowdBattle<T extends Entity> extends Battle
{
	public static final Identifier ID	= Reference.ModInfo.prefix("crowd");
	private final EntityType<T> type;
	private final int maxCount, minCount;
	
	public CrowdBattle(Identifier registryNameIn)
	{
		this(registryNameIn, null, 1, 1);
	}
	
	protected CrowdBattle(Identifier nameIn, EntityType<T> typeIn, int min, int max)
	{
		super(nameIn);
		type = typeIn;
		minCount = min;
		maxCount = max;
	}
	
	public static <J extends Entity> CrowdBattle<J> of(EntityType<J> typeIn)
	{
		return of(typeIn, 1);
	}
	
	public static <J extends Entity> CrowdBattle<J> of(EntityType<J> typeIn, int count)
	{
		return of(typeIn, count, count);
	}
	
	public static <J extends Entity> CrowdBattle<J> of(EntityType<J> typeIn, int min, int max)
	{
		return new CrowdBattle<>(ID, typeIn, min, max);
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
	protected Battle readFromJson(JsonOps ops, JsonObject obj)
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
		
		return new CrowdBattle(ID, type, min, max);
	}
}