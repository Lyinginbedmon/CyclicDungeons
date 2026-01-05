package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.data.ReloadListener;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.JsonOps;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class CDThemes implements ReloadListener<List<JsonObject>>
{
	private static CDThemes INSTANCE;
	private final Map<Identifier, Supplier<Theme>> REGISTRY = new HashMap<>();
	
	public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	public static final String FILE_PATH = "themes";
	
	public static final Identifier ID_GENERIC	= prefix("generic");
	
	public static CDThemes instance() { return INSTANCE; }
	
	public static void init()
	{
		INSTANCE = new CDThemes();
		ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INSTANCE, INSTANCE.getId());
		CyclicDungeons.LOGGER.info(" # Initialised dungeon theme registry");
	}
	
	public Identifier getId()
	{
		return prefix(FILE_PATH);
	}
	
	public void register(@Nullable Theme themeIn)
	{
		if(themeIn == null)
			return;
		
		REGISTRY.put(themeIn.registryName(), () -> themeIn);
		CyclicDungeons.LOGGER.info(" ## Loaded {}", themeIn.registryName().toString());
	}
	
	public Optional<Theme> get(Identifier id)
	{
		return REGISTRY.containsKey(id) ? Optional.of(REGISTRY.get(id).get()) : Optional.empty();
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
				catch(Exception e) { CyclicDungeons.LOGGER.error("Error while loading theme "+fileName.toString()); }
			});
			return objects;
		});
	}
	
	public CompletableFuture<Void> apply(List<JsonObject> data, ResourceManager manager, Executor executor)
	{
		return CompletableFuture.runAsync(() -> 
		{
			CyclicDungeons.LOGGER.info(" # Loading themes from datapack", REGISTRY.size());
			REGISTRY.clear();
			data.forEach(prep -> register(Theme.fromJson(JsonOps.INSTANCE, prep)));
			CyclicDungeons.LOGGER.info(" # {} themes loaded", REGISTRY.size());
		});
	}
}
