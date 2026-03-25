package com.lying.worldgen.theme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.init.CDTerms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

public class InitialPhrase
{
	public static final Codec<InitialPhrase> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("Name").forGetter(InitialPhrase::registryName),
			PhraseTerm.CODEC.listOf().fieldOf("Rooms").forGetter(InitialPhrase::contents)
			).apply(instance, InitialPhrase::new));
	protected final Identifier registryName;
	protected final List<PhraseTerm> entries = Lists.newArrayList();
	
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
		entries.removeIf(term::same);
		entries.add(term);
		return this;
	}
	
	public Identifier registryName() { return registryName; }
	
	protected List<PhraseTerm> contents() { return entries.stream().filter(PhraseTerm::needsRecordingInJSON).toList(); }
	
	public int size() { return entries.size(); }
	
	public GrammarPhrase toPhrase()
	{
		Map<String, GrammarRoom> stock = new HashMap<>();
		for(PhraseTerm term : entries)
			stock.put(term.name(), term.asRoom());
		
		// Interconnect rooms
		for(PhraseTerm term : entries)
		{
			GrammarRoom room = stock.get(term.name());
			term.connections().stream()
				.map(s -> 
				{
					if(!stock.containsKey(s))
					{
						PhraseTerm t = PhraseTerm.of(s);
						stock.put(s, t.asRoom());
						
					}
					return stock.get(s);
				})
				.forEach(room::linkTo);
		}
		
		GrammarPhrase graph = new GrammarPhrase();
		stock.values().forEach(graph::add);
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
					List<String> doors = Lists.newArrayList();
					doorList.ifPresent(doors::addAll);
					door.ifPresent(doors::add);
					return new PhraseTerm(name,content,Optional.of(doors));
				}));
		
		public static PhraseTerm of(String name)
		{
			return new PhraseTerm(name, Optional.empty(), Optional.empty());
		}
		
		public boolean same(PhraseTerm term) { return term.name.equalsIgnoreCase(name); }
		
		public Identifier type() { return content.orElse(CDTerms.ID_BLANK); }
		
		public GrammarRoom asRoom()
		{
			GrammarRoom room = new GrammarRoom();
			room.metadata().setType(type());
			return room;
		}
		
		public List<String> connections() { return doors.orElse(List.of()); }
		
		protected int doorCount() { return doors.orElse(List.of()).size(); }
		
		public boolean needsRecordingInJSON() { return content.isPresent() || doors.isPresent(); }
	}
}
