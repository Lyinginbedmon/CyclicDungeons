package com.lying.utility;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.gson.JsonElement;

public abstract class AbstractMatcherPredicate<T> implements Predicate<T>
{
	/** List of type-specific ListMatcher objects for each internal Optional value */
	protected final List<ListMatcher<T, ?>> matchers;
	
	protected AbstractMatcherPredicate(List<ListMatcher<T, ?>> matchersIn)
	{
		matchers = List.copyOf(matchersIn);
	}
	
	public abstract JsonElement toJson();
	
	public final boolean isEmpty()
	{
		return matchers.stream().allMatch(ListMatcher::isEmpty);
	}
	
	public boolean apply(T input)
	{
		return isEmpty() || matchers.stream().allMatch(v -> v.match(input));
	}
}