package com.lying.grammar;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

/** Predicate handler class used by {@link GrammarTerm} for determining viability in a {@link GrammarRoom} */
public class TermConditions
{
	private boolean afterSelf = true;
	private boolean deadEnds = true;
	private int maxPop = -1, sizeCap = -1;
	private int depthMin = -1;
	private List<Supplier<GrammarTerm>> after = Lists.newArrayList(), before = Lists.newArrayList();
	private List<Supplier<GrammarTerm>> notAfter = Lists.newArrayList(), notBefore = Lists.newArrayList();
	
	protected TermConditions() { }
	
	public static TermConditions create() { return new TermConditions(); }
	
	public TermConditions nonconsecutive(boolean val)
	{
		afterSelf = val;
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

	public TermConditions onlyAfter(List<Supplier<GrammarTerm>> term)
	{
		after.addAll(term);
		return this;
	}

	public TermConditions neverAfter(List<Supplier<GrammarTerm>> term)
	{
		notAfter.addAll(term);
		return this;
	}

	public TermConditions onlyBefore(List<Supplier<GrammarTerm>> term)
	{
		before.addAll(term);
		return this;
	}
	
	public TermConditions neverBefore(List<Supplier<GrammarTerm>> term)
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
		
		final Predicate<GrammarTerm> prevCheck = t -> GrammarTerm.checkListFor(previous, t);
		final Predicate<GrammarTerm> nextCheck = t -> GrammarTerm.checkListFor(next, t);
		
		// Cannot be placed consecutively
		if(!afterSelf && (prevCheck.test(term) || nextCheck.test(term)))
			return false;
		
		// Must be placed after one or more possible rooms
		if(!after.isEmpty() && terms(after).noneMatch(prevCheck::test))
			return false;
		
		// Must be placed before one or more possible rooms
		if(!before.isEmpty() && terms(before).noneMatch(nextCheck::test))
			return false;
		
		// Must not be placed after one or more possible rooms
		if(!notAfter.isEmpty() && terms(notAfter).anyMatch(prevCheck::test))
			return false;
		
		// Must not be placed before one or more possible rooms
		if(!notBefore.isEmpty() && terms(notBefore).anyMatch(nextCheck::test))
			return false;
		
		return true;
	}
	
	private static Stream<GrammarTerm> terms(List<Supplier<GrammarTerm>> setIn)
	{
		return setIn.stream().map(Supplier::get).filter(Objects::nonNull);
	}
}
