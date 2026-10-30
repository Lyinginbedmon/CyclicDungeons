package com.lying.worldgen.theme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.lying.CyclicDungeons;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.init.CDLoggers;
import com.lying.init.CDTerms;
import com.lying.utility.logging.DebugLogger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

public class InitialPhrase
{
	public static final Codec<InitialPhrase> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("Name").forGetter(InitialPhrase::registryName),
			PhraseTerm.CODEC.listOf().fieldOf("Rooms").forGetter(InitialPhrase::contents)
			).apply(instance, InitialPhrase::new));
	public static final DebugLogger LOGGER = CDLoggers.GRAMMAR;
	protected final Identifier registryName;
	protected final Map<String, PhraseTerm> entries = new HashMap<>();
	
	public InitialPhrase(Identifier name)
	{
		registryName = name;
	}
	
	public InitialPhrase(Identifier name, Collection<PhraseTerm> terms)
	{
		this(name);
		terms.forEach(this::add);
	}
	
	public InitialPhrase add(PhraseTerm term)
	{
		entries.put(term.name(), term);
		return this;
	}
	
	public Identifier registryName() { return registryName; }
	
	protected List<PhraseTerm> contents() { return entries.values().stream().filter(PhraseTerm::needsRecordingInJSON).toList(); }
	
	@Nullable
	public static InitialPhrase validate(InitialPhrase phrase)
	{
		/*
		 * Rule 1: There is always a START room
		 * Rule 2: There is only ever 1 START room
		 * Rule 3: START rooms must not be parented
		 * Rule 4: All other rooms must be parented
		 */
		final Logger errorLog = CyclicDungeons.LOGGER;
		if(phrase.entries.isEmpty())
		{
			errorLog.error("Failed to load initial phrase {}: no entries found", phrase.registryName().toString());
			return null;
		}
		
		// All terms in the phrase, specified or otherwise
		final Map<String,PhraseTerm> stock = getFullStock(phrase.entries);
		
		final Function<Collection<PhraseTerm>,InitialPhrase> builder = set -> validate(new InitialPhrase(phrase.registryName(), set));
		
		// Modified phrase contents, if any
		final Map<String,PhraseTerm> entries = new HashMap<>();
		entries.putAll(stock);
		
		// Case 1: No START room, add one
		Optional<PhraseTerm> startOpt = stock.values().stream().filter(PhraseTerm::isStart).findFirst();
		if(startOpt.isEmpty())
		{
			errorLog.warn("Error while loading initial phrase {}: no start room declared", phrase.registryName().toString());
			
			Map<PhraseTerm, String> paternity = calculatePaternity(stock);
			List<PhraseTerm> orphans = stock.values().stream().filter(Predicates.not(paternity::containsKey)).toList();
			
			// If there exists a BLANK orphan, assign it as the START term
			Optional<PhraseTerm> foster = orphans.stream().filter(t -> t.content().isEmpty()).findFirst();
			if(foster.isPresent())
			{
				final PhraseTerm opt = foster.get();
				entries.put(opt.name(), new PhraseTerm(opt.name(), Optional.of(CDTerms.ID_START), Optional.of(opt.connections())));
				return builder.apply(entries.values());
			}
			
			// Assign an orphan term as the child of the new START term, or pick the first term in the list instead if there aren't any orphans
			PhraseTerm target = orphans.isEmpty() ? stock.values().stream().findFirst().get() : orphans.getFirst();
			
			String name = String.valueOf(UUID.randomUUID().getLeastSignificantBits());
			PhraseTerm start = new PhraseTerm(name, Optional.of(CDTerms.ID_START), Optional.of(List.of(target.name())));
			entries.put(start.name(), start);
			
			return builder.apply(entries.values());
		}
		
		// Case 2: Multiple START rooms. Identify most connected and blank the rest
		if(stock.values().stream().filter(PhraseTerm::isStart).count() > 1)
		{
			errorLog.warn("Error while loading initial phrase {}: multiple start rooms declared", phrase.registryName().toString());
			
			Map<PhraseTerm, String> paternity = calculatePaternity(stock);
			PhraseTerm bestStart = null;
			int mostConnections = 0;
			for(PhraseTerm opt : stock.values().stream().filter(PhraseTerm::isStart).toList())
			{
				final int connections = opt.connections().size();
				// START rooms have to be orphaned, so blank any non-orphan START room as writ
				if(!paternity.containsKey(opt) && connections > mostConnections)
				{
					// If this START room is better connected, blank our current best candidate
					if(bestStart != null)
						entries.put(bestStart.name(), bestStart.toBlank());
					
					mostConnections = connections;
					bestStart = opt;
				}
				// If this START room is less connected, blank it
				else
					entries.put(opt.name(), opt.toBlank());
			}
			
			return builder.apply(entries.values());
		}
		
		// Case 3: START room is not orphaned. Detach it from its parent
		final PhraseTerm start = startOpt.get();
		
		/** Map of every term to its immediate parent */
		Map<PhraseTerm, String> paternity = calculatePaternity(stock);
		if(paternity.containsKey(start))
		{
			errorLog.warn("Error while loading initial phrase {}: start room declared as child of another room", phrase.registryName().toString());
			
			final PhraseTerm parent = entries.get(paternity.get(start));
			entries.put(parent.name(), new PhraseTerm(
					parent.name(), 
					parent.content(), 
					Optional.of(parent.connections().stream()
						.filter(Predicates.not(start.name()::equals))
						.toList())));
			
			return builder.apply(entries.values());
		}
		
		// Case 4: Multiple orphan rooms. Prune all orphans
		if((stock.size() - paternity.size()) > 1)
		{
			stock.values().stream()
				.filter(Predicates.not(paternity::containsKey))
				.filter(PhraseTerm::nonStart)
				.forEach(term -> entries.remove(term.name()));
			
			return builder.apply(entries.values());
		}
		
		// If we get through all rules unscathed, just return the input phrase
		return phrase;
	}
	
	/** Returns a map of all terms in the given phrase, including undeclared ones */
	protected static Map<String,PhraseTerm> getFullStock(Map<String,PhraseTerm> entries)
	{
		Map<String,PhraseTerm> stock = new HashMap<>();
		for(PhraseTerm term : entries.values())
		{
			stock.put(term.name(), term);
			for(String opt : term.connections())
				stock.put(opt, entries.getOrDefault(opt, PhraseTerm.of(opt)));
		}
		
		return stock;
	}
	
	/** Returns a map of all terms to their parent */
	protected static Map<PhraseTerm,String> calculatePaternity(Map<String,PhraseTerm> entries)
	{
		Map<PhraseTerm, String> paternity = new HashMap<>();
		for(PhraseTerm term : entries.values())
			for(PhraseTerm opt : entries.values())
				if(opt.connections().contains(term.name()))
				{
					paternity.put(term, opt.name());
					break;
				}
		return paternity;
	}
	
	public int size() { return entries.size(); }
	
	public GrammarPhrase toPhrase()
	{
		List<PhraseTerm> toAdd = new ArrayList<>();
		entries.values().stream().filter(PhraseTerm::isStart).findAny().ifPresent(toAdd::add);
		
		// Recursive approach walking through connections, precludes orphan rooms
		LOGGER.info("Parsing phrase from stock:");
		final GrammarPhrase graph = new GrammarPhrase();
		Map<String, GrammarRoom> stock = new HashMap<>();
		while(!toAdd.isEmpty())
		{
			List<PhraseTerm> next = new ArrayList<>();
			for(PhraseTerm term : toAdd)
			{
				final String name = term.name();
				LOGGER.info(" # {}", term.toString());
				
				final GrammarRoom room = stock.getOrDefault(name, term.asRoom()).setCustomName(name);
				stock.put(name, room);
				
				for(String child : term.connections())
				{
					PhraseTerm t2 = entries.getOrDefault(child, PhraseTerm.of(child));
					next.add(t2);
					
					GrammarRoom r2 = stock.getOrDefault(child, t2.asRoom()).setCustomName(child);
					room.linkTo(r2);
					stock.put(child, r2);
				}
				
				graph.add(room);
			}
			
			toAdd.clear();
			toAdd.addAll(next);
		}
		
		return graph;
	}
	
	public static record PhraseTerm(String name, Optional<Identifier> content, Optional<List<String>> doors)
	{
		public static final Codec<PhraseTerm> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("Name").forGetter(PhraseTerm::name),
				Identifier.CODEC.optionalFieldOf("Content").forGetter(PhraseTerm::content),
				Codec.STRING.listOf().optionalFieldOf("Connections").forGetter(t -> t.doorCount() > 1 ? t.doors() : Optional.empty()),
				Codec.STRING.optionalFieldOf("Connection").forGetter(t -> t.doorCount() == 1 ? Optional.of(t.doors().get().getFirst()) : Optional.empty())
				).apply(instance, (name,content,doorList,door) -> 
				{
					List<String> doors = new ArrayList<>();
					doorList.ifPresent(doors::addAll);
					door.ifPresent(doors::add);
					return new PhraseTerm(name,content,Optional.of(doors));
				}));
		
		public static PhraseTerm of(String name)
		{
			return new PhraseTerm(name, Optional.empty(), Optional.empty());
		}
		
		public boolean equals(PhraseTerm term) { return term.name.equalsIgnoreCase(name); }
		
		public String toString()
		{
			String name = name();
			if(content.isPresent())
				name += " {" + content.get().toString() +"}";
			if(doorCount() > 0)
				name += ", parent of " + String.join(", ", doors.get());
			return name;
		}
		
		public Identifier type() { return content.orElse(CDTerms.ID_BLANK); }
		
		public boolean isStart() { return content.isPresent() && content.get().equals(CDTerms.ID_START); }
		
		public boolean nonStart() { return !isStart(); }
		
		public PhraseTerm toBlank() { return new PhraseTerm(name, Optional.empty(), doors); }
		
		public GrammarRoom asRoom()
		{
			GrammarRoom room = new GrammarRoom();
			content.ifPresent(id -> room.metadata().setType(id));
			return room;
		}
		
		public List<String> connections() { return doors.orElse(List.of()); }
		
		protected int doorCount() { return doors.orElse(List.of()).size(); }
		
		public boolean needsRecordingInJSON() { return content.isPresent() || doors.isPresent(); }
	}
}
