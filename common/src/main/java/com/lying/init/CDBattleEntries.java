package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.data.ReloadListener;
import com.lying.grammar.content.battle.BattleEntry;
import com.mojang.serialization.JsonOps;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class CDBattleEntries implements ReloadListener<List<JsonObject>>
{
	private static CDBattleEntries INSTANCE;
	private final Map<Identifier, BattleEntry> REGISTRY	= new HashMap<>();
	
	public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	public static final String FILE_PATH = "encounters";
	
	public static CDBattleEntries instance() { return INSTANCE; }
	
	public static void init()
	{
		INSTANCE = new CDBattleEntries();
		ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INSTANCE, INSTANCE.getId());
		CyclicDungeons.LOGGER.info(" # Initialised encounter entry registry");
	}
	
	public Identifier getId()
	{
		return prefix(FILE_PATH);
	}
	
	public Optional<BattleEntry> get(Identifier id)
	{
		BattleEntry entry;
		return REGISTRY.containsKey(id) && (entry = REGISTRY.get(id)) != null ? Optional.of(entry) : Optional.empty();
	}
	
	public void register(BattleEntry entry)
	{
		if(entry == null)
			return;
		REGISTRY.put(entry.registryName(), entry);
		CyclicDungeons.LOGGER.info(" ## Loaded {}", entry.registryName().toString());
	}
	
	public CompletableFuture<List<JsonObject>> load(ResourceManager manager)
	{
		return CompletableFuture.supplyAsync(() -> 
		{
			List<JsonObject> objects = Lists.newArrayList();
			manager.findAllResources(FILE_PATH, Predicates.alwaysTrue()).forEach((fileName,fileSet) -> 
			{
				Resource file = fileSet.getFirst();
				try
				{
					objects.add(JsonHelper.deserialize(GSON, (Reader)file.getReader(), JsonObject.class));
				}
				catch(Exception e) { CyclicDungeons.LOGGER.error("Error while loading encounter entry "+fileName.toString()); }
			});
			return objects;
		});
	}
	
	public CompletableFuture<Void> apply(List<JsonObject> data, ResourceManager manager, Executor executor)
	{
		return CompletableFuture.runAsync(() -> 
		{
			CyclicDungeons.LOGGER.info(" # Loading encounter entries from datapack", REGISTRY.size());
			REGISTRY.clear();
			data.forEach(prep -> register(BattleEntry.CODEC.parse(JsonOps.INSTANCE, prep).getOrThrow()));
			CyclicDungeons.LOGGER.info(" # {} encounter entries loaded", REGISTRY.size());
		});
	}
}
