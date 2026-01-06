package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.data.ReloadListener;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.TermConditions;
import com.lying.grammar.modifier.PhraseModifier;
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
	public static final String FILE_PATH = "terms";
	
	private static CDTerms INSTANCE;
	
	public static final Identifier
		ID_START			= prefix("start"),
		ID_END				= prefix("end"),
		ID_VOID				= prefix("void"),
		ID_INJECT_ROOM		= prefix("inject_room"),
		ID_INJECT_BRANCH	= prefix("inject_branch"),
		ID_EMPTY			= prefix("empty"),
		ID_BLANK			= prefix("blank"),
		ID_TREASURE			= prefix("treasure");
	
	public CDTerms()
	{
		reset();
	}
	
	public GrammarTerm register(Identifier id, GrammarTerm.Builder funcIn)
	{
		return register(funcIn.build(id));
	}
	
	public GrammarTerm register(GrammarTerm supplierIn)
	{
		registerSilent(supplierIn);
		CyclicDungeons.LOGGER.info(" ## Loaded {}", supplierIn.registryName().toString());
		return supplierIn;
	}
	
	private GrammarTerm registerSilent(Identifier id, GrammarTerm.Builder funcIn)
	{
		return registerSilent(funcIn.build(id));
	}
	
	private GrammarTerm registerSilent(GrammarTerm term)
	{
		Identifier id = term.registryName();
		REGISTRY.put(id, term);
		return term;
	}
	
	public Optional<GrammarTerm> parse(String name) { return get(name.contains(":") ? Identifier.of(name) : prefix(name)); }
	
	public Optional<GrammarTerm> get(Identifier id) { return REGISTRY.containsKey(id) ? Optional.of(REGISTRY.get(id)) : Optional.empty(); }
	
	public List<GrammarTerm> placeables() { return REGISTRY.values().stream().filter(GrammarTerm::isPlaceable).toList(); }
	
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
	public final GrammarTerm blank() { return tryGetEvergreen(ID_BLANK); }
	public final GrammarTerm nan() { return tryGetEvergreen(ID_VOID); }
	
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
		
		// Initial building blocks
		registerSilent(ID_START, GrammarTerm.Builder.create(0xFFFFFF)
				.size(new Vector2i(6, 8))
				.unplaceable());
		registerSilent(ID_END, GrammarTerm.Builder.create(0xFFFFFF)
				.size(new Vector2i(6, 8))
				.unplaceable());
		
		/** Completely blank, only used to mark errors in generation */
		registerSilent(ID_VOID, GrammarTerm.Builder.create(0x000000)
				.unplaceable());
		
		// Replaceable rooms, only occur during generation
		registerSilent(ID_BLANK, GrammarTerm.Builder.create(0x080808)
				.unplaceable()
				.replaceable());
		registerSilent(ID_INJECT_ROOM, GrammarTerm.Builder.create(0xD2D2D2)
				.withCondition(TermConditions.create()
					.sizeCap(6))
				.replaceable()
				.weight(3)
				.onApply(PhraseModifier.INJECT_ROOM.get()));
		registerSilent(ID_INJECT_BRANCH, GrammarTerm.Builder.create(0xB9B9B9)
				.withCondition(TermConditions.create()
					.sizeCap(6))
				.replaceable()
				.weight(4)
				.onApply(PhraseModifier.INJECT_BRANCH.get()));
		
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
			List<JsonObject> objects = Lists.newArrayList();
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
			CyclicDungeons.LOGGER.info(" # {} grammar terms loaded ({} placeable)", REGISTRY.size(), placeables().size());
		});
	}
}
