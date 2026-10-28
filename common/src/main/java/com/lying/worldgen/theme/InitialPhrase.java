package com.lying.worldgen.theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	
	public InitialPhrase(Identifier name, List<PhraseTerm> terms)
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
		
		// FIXME Resolve loading errors in stock phrases
		
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
