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
	public static final Codec<InitialPhrase> CODEC	= PhraseTerm.CODEC.listOf().xmap(InitialPhrase::of, InitialPhrase::contents);
	protected final List<PhraseTerm> entries = Lists.newArrayList();
	
	public static InitialPhrase of(List<PhraseTerm> terms)
	{
		InitialPhrase phrase = new InitialPhrase();
		terms.forEach(phrase::add);
		return phrase;
	}
	
	public InitialPhrase add(PhraseTerm term)
	{
		entries.removeIf(term::same);
		entries.add(term);
		return this;
	}
	
	protected List<PhraseTerm> contents() { return entries; }
	
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
				.filter(stock::containsKey)
				.map(stock::get)
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
				Codec.STRING.listOf().optionalFieldOf("Connections").forGetter(PhraseTerm::doors)
				).apply(instance, PhraseTerm::new));
		
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
	}
}
