package com.lying.utility;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/** Helper class for testing one object against a stream of list values of another object class */
public class ListMatcher<S extends Object, T extends Object>
{
	private final Optional<List<T>> values;
	private final BiPredicate<S, Stream<T>> handlerFunc;
	
	public ListMatcher(Optional<List<T>> valuesIn, BiPredicate<S, Stream<T>> handlerFuncIn)
	{
		values = valuesIn;
		handlerFunc = handlerFuncIn;
	}
	
	public final boolean isPresent() { return values.isPresent(); }
	
	public final boolean isEmpty() { return values.isEmpty(); }
	
	public final boolean match(S state)
	{
		return values.isEmpty() || handlerFunc.test(state, values.get().stream());
	}
}