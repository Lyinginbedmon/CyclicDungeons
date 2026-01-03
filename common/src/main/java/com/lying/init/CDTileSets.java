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
import com.lying.worldgen.tileset.TileSet;
import com.mojang.serialization.JsonOps;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class CDTileSets implements ReloadListener<List<JsonObject>>
{
	private static CDTileSets INSTANCE;
	private final Map<Identifier, TileSet> REGISTRY	= new HashMap<>();
	
	public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	public static final String FILE_PATH = "tile_sets";
	
	public static final TileSet DEFAULT	= new TileSet(prefix("default_if_unrecognised"))
			.add(CDTiles.ID_AIR, 10F)
			.add(CDTiles.ID_STONE, 1000F);
	
	public static CDTileSets instance() { return INSTANCE; }
	
	public static void init()
	{
		INSTANCE = new CDTileSets();
		ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INSTANCE, INSTANCE.getId());
		CyclicDungeons.LOGGER.info(" # Initialised tile set registry");
	}
	
	public Identifier getId()
	{
		return prefix(FILE_PATH);
	}
	
	public Optional<TileSet> get(Identifier id)
	{
		return REGISTRY.containsKey(id) ? Optional.of(REGISTRY.get(id)) : Optional.empty();
	}
	
	public void register(TileSet tileSet)
	{
		REGISTRY.put(tileSet.registryName(), tileSet);
		CyclicDungeons.LOGGER.info(" ## Loaded {}", tileSet.registryName().toString());
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
				catch(Exception e) { CyclicDungeons.LOGGER.error("Error while loading tile set "+fileName.toString()); }
			});
			return objects;
		});
	}
	
	public CompletableFuture<Void> apply(List<JsonObject> data, ResourceManager manager, Executor executor)
	{
		return CompletableFuture.runAsync(() -> 
		{
			CyclicDungeons.LOGGER.info(" # Loading tile sets from datapack", REGISTRY.size());
			REGISTRY.clear();
			data.forEach(prep -> register(TileSet.decode(JsonOps.INSTANCE, prep)));
			CyclicDungeons.LOGGER.info(" # {} tile sets loaded", REGISTRY.size());
		});
	}
}
