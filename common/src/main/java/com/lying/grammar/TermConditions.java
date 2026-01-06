package com.lying.grammar;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

/** Predicate handler class used by {@link GrammarTerm} for determining viability in a {@link GrammarRoom} */
public class TermConditions
{
	public static final Codec<TermConditions> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("consecutive").forGetter(t -> t.consecutive),
			Codec.BOOL.fieldOf("dead_ends").forGetter(t -> t.deadEnds),
			Codec.INT.optionalFieldOf("max_count").forGetter(t -> t.maxPop > 0 ? Optional.of(t.maxPop) : Optional.empty()),
			Codec.INT.optionalFieldOf("before_dungeon_size").forGetter(t -> t.sizeCap > 0 ? Optional.of(t.sizeCap) : Optional.empty()),
			Identifier.CODEC.listOf().optionalFieldOf("only_after").forGetter(t -> t.after.isEmpty() ? Optional.empty() : Optional.of(t.after)),
			Identifier.CODEC.listOf().optionalFieldOf("only_before").forGetter(t -> t.before.isEmpty() ? Optional.empty() : Optional.of(t.before)),
			Identifier.CODEC.listOf().optionalFieldOf("never_after").forGetter(t -> t.notAfter.isEmpty() ? Optional.empty() : Optional.of(t.notAfter)),
			Identifier.CODEC.listOf().optionalFieldOf("never_before").forGetter(t -> t.notBefore.isEmpty() ? Optional.empty() : Optional.of(t.notBefore))
			).apply(instance, (consecutive, deadEnds, maxPop, sizeCap, after, before, notAfter, notBefore) -> 
			{
				TermConditions conditions = new TermConditions();
				conditions.consecutive(consecutive);
				conditions.allowDeadEnds(deadEnds);
				maxPop.ifPresent(i -> conditions.popCap(i));
				sizeCap.ifPresent(i -> conditions.sizeCap(i));
				after.ifPresent(set -> conditions.onlyAfter(set));
				before.ifPresent(set -> conditions.onlyBefore(set));
				notAfter.ifPresent(set -> conditions.neverAfter(set));
				notBefore.ifPresent(set -> conditions.neverBefore(set));
				return conditions;
			}));
	
	private boolean consecutive = true;
	private boolean deadEnds = true;
	private int maxPop = -1, sizeCap = -1;
	private int depthMin = -1;
	private List<Identifier> after = Lists.newArrayList(), before = Lists.newArrayList();
	private List<Identifier> notAfter = Lists.newArrayList(), notBefore = Lists.newArrayList();
	
	protected TermConditions() { }
	
	public static TermConditions create() { return new TermConditions(); }
	
	public TermConditions consecutive(boolean val)
	{
		consecutive = val;
		return this;
	}
	
	public TermConditions afterDepth(int dep)
	{
		depthMin = dep;
		return this;
	}
	
	public TermConditions allowDeadEnds(boolean val)
	{
		deadEnds = val;
		return this;
	}
	
	public TermConditions popCap(int cap)
	{
		maxPop = cap;
		return this;
	}
	
	public TermConditions sizeCap(int cap)
	{
		sizeCap = cap;
		return this;
	}
	
	public TermConditions onlyAfter(List<Identifier> term)
	{
		after.addAll(term);
		return this;
	}
	
	public TermConditions neverAfter(List<Identifier> term)
	{
		notAfter.addAll(term);
		return this;
	}
	
	public TermConditions onlyBefore(List<Identifier> term)
	{
		before.addAll(term);
		return this;
	}
	
	public TermConditions neverBefore(List<Identifier> term)
	{
		notBefore.addAll(term);
		return this;
	}
	
	public boolean test(GrammarTerm term, GrammarRoom inRoom, List<GrammarRoom> previous, List<GrammarRoom> next, GrammarPhrase graph)
	{
		// Only placeable whilst graph is below a maximum scale
		if(sizeCap > 0 && graph.size() >= sizeCap)
			return false;
		
		// Only placeable after a minimum number of preceding rooms
		if(depthMin > 0 && inRoom.metadata().depth() <= depthMin)
			return false;
		
		// Only placeable up to a maximum population in the same graph
		if(maxPop > 0 && graph.tally(term) >= maxPop)
			return false;
		
		// Cannot be placed at a dead end
		if(!deadEnds && !inRoom.hasLinks())
			return false;
		
		// Cannot be placed consecutively
		if(!consecutive)
		{
			if(!previous.isEmpty() && previous.getLast().metadata().is(term))
				return false;
			
			if(!next.isEmpty() && next.getFirst().metadata().is(term))
				return false;
		}
		
		Stream<Identifier> nextRooms = next.stream()
				.map(GrammarRoom::metadata)
				.map(RoomMetadata::type)
				.map(GrammarTerm::registryName);
		Stream<Identifier> prevRooms = previous.stream()
				.map(GrammarRoom::metadata)
				.map(RoomMetadata::type)
				.map(GrammarTerm::registryName);
		
		// Must be placed after one or more possible rooms
		if(!after.isEmpty() && prevRooms.noneMatch(after::contains))
			return false;
		
		// Must be placed before one or more possible rooms
		if(!before.isEmpty() && nextRooms.noneMatch(before::contains))
			return false;
		
		// Must not be placed after one or more possible rooms
		if(!notAfter.isEmpty() && prevRooms.anyMatch(notAfter::contains))
			return false;
		
		// Must not be placed before one or more possible rooms
		if(!notBefore.isEmpty() && nextRooms.anyMatch(notBefore::contains))
			return false;
		
		return true;
	}
}
