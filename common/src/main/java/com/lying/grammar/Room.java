package com.lying.grammar;

import java.util.Optional;
import java.util.UUID;

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
		return term.name();
	}
	
	public Optional<UUID> getLink() { return linksTo; }
	
	public Room linkTo(UUID otherRoom)
	{
		linksTo = Optional.of(otherRoom);
		return this;
	}
	
	public boolean hasLink() { return linksTo.isPresent(); }
	
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