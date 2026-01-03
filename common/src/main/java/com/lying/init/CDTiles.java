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

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.data.ReloadListener;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tile.TilePredicate;
import com.lying.worldgen.tile.condition.Boundary;
import com.lying.worldgen.tile.condition.Not;
import com.mojang.serialization.JsonOps;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;

public class CDTiles implements ReloadListener<List<JsonObject>>
{
	private static CDTiles INSTANCE;
	private final Map<Identifier, Supplier<Tile>> REGISTRY = new HashMap<>();
	
	public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	public static final String FILE_PATH = "tiles";
	
	public static final Identifier 
		ID_BLANK			= prefix("blank"), 
		ID_AIR				= prefix("air"),
		ID_STONE			= prefix("stone"),
		ID_PASSAGE_FLAG			= prefix("passage_flag"),
		ID_DOORWAY			= prefix("doorway"),
		ID_DOORWAY_LINTEL	= prefix("doorway_lintel");
	
	/** Blank tile, used during generation */
	public static final Supplier<Tile> BLANK	= () -> Tile.Builder
			.of(TilePredicate.fromCondition(CDTileConditions.NEVER.get()))
			.asFlag().build().apply(ID_BLANK);
	
	/** Air tile, hard-coded default if no other is found */
	public static final Supplier<Tile> AIR		= () -> Tile.Builder
			.of(TilePredicate.fromCondition(Not.of(CDTileConditions.ON_BOTTOM.get())))
			.asAir().build().apply(ID_AIR);
	
	/** Solid rock, hard-coded default if no other is found */
	public static final Supplier<Tile> STONE	= () -> Tile.Builder
			.of(TilePredicate.fromCondition(CDTileConditions.ON_BOTTOM.get()))
			.asBlock(Blocks.STONE.getDefaultState()).build().apply(ID_STONE);
	
	/** Flag tile placed either side of doorways to ensure navigability */
	public static final Supplier<Tile> PASSAGE_FLAG	= () -> Tile.Builder
			.of(TilePredicate.fromCondition(Boundary.of(Direction.Type.HORIZONTAL)))
			.asAir().build().apply(ID_PASSAGE_FLAG);
	
	public static final Supplier<Tile> DOORWAY	= () -> Tile.Builder
			.of(TilePredicate.fromCondition(Boundary.of(Direction.Type.HORIZONTAL)))
			.asStructure()
			.build().apply(ID_DOORWAY);
	
	public static final Supplier<Tile> DOORWAY_LINTEL	= () -> Tile.Builder
			.of(TilePredicate.fromCondition(Boundary.of(Direction.Type.HORIZONTAL)))
			.asStructure()
			.build().apply(ID_DOORWAY_LINTEL);
	
	public static CDTiles instance() { return INSTANCE; }
	
	public static void init()
	{
		INSTANCE = new CDTiles();
		ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INSTANCE, INSTANCE.getId());
		CyclicDungeons.LOGGER.info(" # Initialised tile registry");
	}
	
	public void register(Tile tileIn)
	{
		REGISTRY.put(tileIn.registryName(), () -> tileIn);
		CyclicDungeons.LOGGER.info(" ## Loaded {}", tileIn.registryName().toString());
	}
	
	public Optional<Tile> get(Identifier id)
	{
		return REGISTRY.containsKey(id) ? Optional.of(REGISTRY.get(id).get()) : Optional.empty();
	}
	
	public Tile getElse(Identifier id, Supplier<Tile> sys)
	{
		return get(id).orElse(sys.get());
	}
	
	public List<Tile> getAll()
	{
		return REGISTRY.values().stream().map(Supplier::get).toList();
	}
	
	public Identifier getId()
	{
		return prefix(FILE_PATH);
	}
	
	protected void reset()
	{
		REGISTRY.clear();
		REGISTRY.put(ID_AIR, AIR);
		REGISTRY.put(ID_STONE, STONE);
		REGISTRY.put(ID_BLANK, BLANK);
		REGISTRY.put(ID_PASSAGE_FLAG, PASSAGE_FLAG);
		REGISTRY.put(ID_DOORWAY, DOORWAY);
		REGISTRY.put(ID_DOORWAY_LINTEL, DOORWAY_LINTEL);
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
				catch(Exception e) { CyclicDungeons.LOGGER.error("Error while loading tile entry "+fileName.toString()); }
			});
			return objects;
		});
	}
	
	public CompletableFuture<Void> apply(List<JsonObject> data, ResourceManager manager, Executor executor)
	{
		return CompletableFuture.runAsync(() -> 
		{
			CyclicDungeons.LOGGER.info(" # Loading tiles from datapack", REGISTRY.size());
			reset();
			data.forEach(prep -> register(Tile.readFromJson(prep, JsonOps.INSTANCE)));
			CyclicDungeons.LOGGER.info(" # Loaded {} tiles from datapack", REGISTRY.size());
			CDTileTags.reload();
		});
	}
}
