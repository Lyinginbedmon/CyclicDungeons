package com.lying.grammar;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.lying.reference.Reference;

import net.minecraft.text.MutableText;

public class Room
{
	private final UUID id;
	public int depth = 0;
	private Term term = CDTerms.BLANK.get();
	private List<UUID> linksTo = Lists.newArrayList();
	
	public Room(UUID idIn)
	{
		id = idIn;
	}
	
	public Room()
	{
		this(UUID.randomUUID());
	}
	
	public static Comparator<Room> branchSort(Graph graph)
	{
		return (a,b) -> 
		{
			int aD = a.tallyDescendants(graph);
			int bD = b.tallyDescendants(graph);
			return aD < bD ? -1 : aD > bD ? 1 : 0;
		};
	}
	
	public final UUID uuid() { return id; }
	
	public final boolean matches(Room b) { return b.id.equals(id); }
	
	public final MutableText name()
	{
		return Reference.ModInfo.translate("debug", "room", term.name(), depth);
	}
	
	public final String asString()
	{
		return term.name().getString()+" ("+depth+")";
	}
	
	public boolean hasLinks() { return !linksTo.isEmpty(); }
	
	public boolean hasLinkTo(UUID uuid) { return !linksTo.isEmpty() && linksTo.contains(uuid); }
	
	public int tallyDescendants(Graph graph)
	{
		int tally = linksTo.size();
		for(UUID offshoot : linksTo)
		{
			Optional<Room> r = graph.get(offshoot);
			if(r.isEmpty())
				continue;
			tally += r.get().tallyDescendants(graph);
		}
		return tally;
	}
	
	public Room linkTo(UUID otherRoom)
	{
		linksTo.add(otherRoom);
		return this;
	}
	
	public Room detachFrom(UUID otherRoom)
	{
		if(hasLinks() && hasLinkTo(otherRoom))
			linksTo.remove(otherRoom);
		return this;
	}
	
	/** Collects all rooms within the given graph that this room links to */
	@NotNull
	public List<Room> getLinksFrom(Graph graph)
	{
		List<Room> links = Lists.newArrayList();
		linksTo.stream()
			.map(graph::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(links::add);
		return links;
	}
	
	public List<UUID> getLinkIds() { return List.of(linksTo.toArray(new UUID[0])); }
	
	/** Returns true if this room can be replaced during generation */
	public boolean isReplaceable() { return term.isReplaceable(); }
	
	public Room applyTerm(Term termIn, Graph graph)
	{
		termIn.applyTo(this, graph);
		return this;
	}
	
	public Room setTerm(Term termIn)
	{
		term = termIn;
		return this;
	}
	
	public Term getTerm() { return term; }
	
	public boolean is(Term term) { return this.term.matches(term); }
}