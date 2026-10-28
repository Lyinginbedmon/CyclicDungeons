package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.data.ReloadListener;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.TermConditions;
import com.mojang.serialization.JsonOps;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class CDTerms implements ReloadListener<List<JsonObject>>
{
	private final Map<Identifier, GrammarTerm> REGISTRY = new HashMap<>();
	
	public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	public static final String FILE_PATH = "grammar/terms";
	
	private static CDTerms INSTANCE;
	
	public static final Identifier
		ID_START			= prefix("start"),
		ID_END				= prefix("end"),
		ID_VOID				= prefix("void"),
		ID_EMPTY			= prefix("empty"),
		ID_BLANK			= prefix("blank");
	
	/** Immutable system terms that cannot be customised via datapacks */
	private static final List<Identifier> IMMUTABLE	= List.of(
			ID_VOID,
			ID_BLANK
			);
	
	/** Replaceable rooms, only occur during generation */
	public static final GrammarTerm BLANK	= GrammarTerm.Builder.create(0x080808)
			.unplaceable()
			.replaceable()
			.build(ID_BLANK);
	/** Completely blank, only used to mark errors in generation */
	public static final GrammarTerm VOID	= GrammarTerm.Builder.create(0x000000)
			.unplaceable()
			.build(ID_VOID);
	
	public CDTerms()
	{
		reset();
	}
	
	@Nullable
	public GrammarTerm register(Identifier id, GrammarTerm.Builder funcIn)
	{
		return register(funcIn.build(id));
	}
	
	@Nullable
	public GrammarTerm register(GrammarTerm supplierIn)
	{
		GrammarTerm term = registerSilent(supplierIn);
		if(term == null)
			return null;
		
		CyclicDungeons.LOGGER.info(" ## Loaded {}", supplierIn.registryName().toString());
		return supplierIn;
	}
	
	@Nullable
	private GrammarTerm registerSilent(Identifier id, GrammarTerm.Builder funcIn)
	{
		return registerSilent(funcIn.build(id));
	}
	
	@Nullable
	private GrammarTerm registerSilent(GrammarTerm term)
	{
		final Identifier id = term.registryName();
		if(IMMUTABLE.contains(id) && !REGISTRY.containsKey(id))
		{
			CyclicDungeons.LOGGER.info(" # Found immutable {} term in datapack, ignored", id.toString());
			return null;
		}
		
		REGISTRY.put(id, term);
		return term;
	}
	
	public Optional<GrammarTerm> parse(String name) { return get(name.contains(":") ? Identifier.of(name) : prefix(name)); }
	
	public Optional<GrammarTerm> get(Identifier id) { return REGISTRY.containsKey(id) ? Optional.of(REGISTRY.get(id)) : Optional.empty(); }
	
	public static void init()
	{
		INSTANCE = new CDTerms();
		ReloadListenerRegistry.register(ResourceType.SERVER_DATA, INSTANCE, INSTANCE.getId());
		CyclicDungeons.LOGGER.info(" # Initialised grammar term registry");
	}
	
	public static CDTerms instance() { return INSTANCE; }
	
	public Identifier getId() { return prefix(FILE_PATH); }
	
	public final GrammarTerm start() { return tryGetEvergreen(ID_START); }
	public final GrammarTerm end() { return tryGetEvergreen(ID_END); }
	
	protected final GrammarTerm tryGetEvergreen(Identifier idIn)
	{
		if(!REGISTRY.containsKey(idIn))
			CyclicDungeons.LOGGER.error("Failed to retrieve evergreen grammar term {}, value missing", idIn.toString());
		
		GrammarTerm term = REGISTRY.get(idIn);
		if(term == null)
			CyclicDungeons.LOGGER.error("Failed to retrieve evergreen grammar term {}, value null", idIn.toString());
		
		return term;
	}
	
	protected void reset()
	{
		REGISTRY.clear();
		
		REGISTRY.put(ID_BLANK, BLANK);
		REGISTRY.put(ID_VOID, VOID);
		
		// Initial building blocks
		registerSilent(ID_START, GrammarTerm.Builder.create(0xFFFFFF)
				.size(new Vector2i(6, 8))
				.unplaceable());
		registerSilent(ID_END, GrammarTerm.Builder.create(0xFFFFFF)
				.size(new Vector2i(6, 8))
				.unplaceable());
		
		// Functional rooms
		registerSilent(ID_EMPTY, GrammarTerm.Builder.create(0xA6A6A6)
				.withCondition(TermConditions.create()
					.consecutive(false)
					.allowDeadEnds(false)
					.neverAfter(List.of(CDTerms.ID_START))
					.neverBefore(List.of(CDTerms.ID_END)))
				.size(new Vector2i(7, 10)));
	}
	
	public CompletableFuture<List<JsonObject>> load(ResourceManager manager)
	{
		return CompletableFuture.supplyAsync(() -> 
		{
			List<JsonObject> objects = new ArrayList<>();
			manager.findAllResources(FILE_PATH, Predicates.alwaysTrue()).forEach((fileName,fileSet) -> 
			{
				Resource file = fileSet.getFirst();
				try
				{
					objects.add(JsonHelper.deserialize(GSON, (Reader)file.getReader(), JsonObject.class));
				}
				catch(Exception e) { CyclicDungeons.LOGGER.error("Error while loading grammar term "+fileName.toString()); }
			});
			return objects;
		});
	}
	
	public CompletableFuture<Void> apply(List<JsonObject> data, ResourceManager manager, Executor executor)
	{
		return CompletableFuture.runAsync(() -> 
		{
			CyclicDungeons.LOGGER.info(" # Loading grammar terms from datapack", REGISTRY.size());
			reset();
			data.forEach(prep -> register(GrammarTerm.readFromJson(JsonOps.INSTANCE, prep)));
			CyclicDungeons.LOGGER.info(" # {} grammar terms loaded ({} placeable)", REGISTRY.size(), REGISTRY.values().stream().filter(GrammarTerm::isPlaceable).count());
		});
	}
}
