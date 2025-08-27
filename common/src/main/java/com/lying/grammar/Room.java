package com.lying.grammar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.lying.reference.Reference;

import net.minecraft.text.MutableText;

public class Room
{
	private final UUID id;
	public int depth = 0;
	private Term term = CDTerms.BLANK.get();
	private Optional<UUID> linksTo = Optional.empty();
	
	public Room(UUID idIn)
	{
		id = idIn;
	}
	
	public Room()
	{
		this(UUID.randomUUID());
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
	
	public boolean hasLinks() { return linksTo.isPresent(); }
	
	public boolean hasLinkTo(UUID uuid) { return linksTo.isPresent() && linksTo.get().equals(uuid); }
	
	public Room linkTo(UUID otherRoom)
	{
		linksTo = Optional.of(otherRoom);
		return this;
	}
	
	public Room detachFrom(UUID otherRoom)
	{
		if(linksTo.isPresent() && linksTo.get().equals(otherRoom))
			linksTo = Optional.empty();
		return this;
	}
	
	/** Collects all rooms within the given graph that this room links to */
	public List<Room> getLinksFrom(Graph graph)
	{
		List<Room> links = Lists.newArrayList();
		getLinkIds().forEach(id -> graph.get(id).ifPresent(links::add));
		return links;
	}
	
	public List<UUID> getLinkIds()
	{
		List<UUID> links = Lists.newArrayList();
		linksTo.ifPresent(links::add);
		return links;
	}
	
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