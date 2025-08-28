package com.lying.grammar;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public abstract class Term
{
	private final Identifier registryName;
	private final EnumSet<EnumTermType> groups;
	private final boolean isReplaceable, isPlaceable;
	
	private Term(Identifier idIn, boolean placeable, boolean replaceable, EnumSet<EnumTermType> groupsIn)
	{
		registryName = idIn;
		isPlaceable = placeable;
		isReplaceable = replaceable;
		groups = groupsIn;
	}
	
	public Identifier registryName() { return registryName; }
	
	public MutableText name() { return Text.literal(registryName.getPath()); }
	
	public boolean matches(Term b) { return registryName.equals(b.registryName); }
	
	/** Returns true if generation should replace rooms with this term */
	public boolean isReplaceable() { return isReplaceable; }
	
	/** Returns true if generation can place this kind of room */
	public boolean isPlaceable() { return isPlaceable; }
	
	/** Returns true if this Term can exist in the given room */
	public abstract boolean canBePlaced(Room inRoom, @NotNull List<Room> previous, @NotNull List<Room> next, Graph graph);
	
	public void applyTo(Room room, Graph graph)
	{
		room.setTerm(this);
		onApply(room, graph);
	}
	
	protected abstract void onApply(Room room, Graph graph);
	
	public static Room injectRoom(Room room, Graph graph)
	{
		Room injected = new Room();
		room.getLinkIds().forEach(uuid -> 
		{
			// Move all links of parent to child
			injected.linkTo(uuid);
			room.detachFrom(uuid);
		});
		// Link parent to child and add to graph
		room.linkTo(injected.uuid());
		graph.add(injected);
		return injected;
	}
	
	public static Room injectBranch(Room room, Graph graph)
	{
		Room injected = new Room();
		room.linkTo(injected.uuid());
		graph.add(injected);
		return injected;
	}
	
	protected static boolean checkListFor(@Nullable List<Room> rooms, Term term)
	{
		return rooms != null && !rooms.isEmpty() && rooms.stream().anyMatch(r -> r.is(term));
	}
	
	@SuppressWarnings("unchecked")
	public static class Builder
	{
		private boolean replaceable = false;
		private boolean placeable = true;
		private boolean afterSelf = true;
		private boolean deadEnds = true;
		private int maxPop = -1, sizeCap = -1;
		private int depthMin = -1;
		private List<Supplier<Term>> after = Lists.newArrayList(), before = Lists.newArrayList();
		private List<Supplier<Term>> notAfter = Lists.newArrayList(), notBefore = Lists.newArrayList();
		private TermApplyFunc applyFunc = (t,r,g) -> {};
		private EnumSet<EnumTermType> groups = EnumSet.noneOf(EnumTermType.class);
		
		private Builder() { }
		
		public static Builder create()
		{
			return new Builder();
		}
		
		public Builder group(EnumTermType groupIn)
		{
			groups.add(groupIn);
			return this;
		}
		
		public Builder unplaceable()
		{
			placeable = false;
			return this;
		}
		
		public Builder replaceable()
		{
			replaceable = true;
			return this;
		}
		
		public Builder nonconsecutive()
		{
			afterSelf = false;
			return this;
		}
		
		public Builder afterDepth(int dep)
		{
			depthMin = dep;
			return this;
		}
		
		public Builder allowDeadEnds(boolean val)
		{
			deadEnds = val;
			return this;
		}
		
		public Builder popCap(int cap)
		{
			maxPop = cap;
			return this;
		}
		
		public Builder sizeCap(int cap)
		{
			sizeCap = cap;
			return this;
		}
		
		public Builder onlyAfter(Supplier<Term>... term)
		{
			for(Supplier<Term> termIn : term)
				after.add(termIn);
			return this;
		}
		
		public Builder neverAfter(Supplier<Term>... term)
		{
			for(Supplier<Term> termIn : term)
				notAfter.add(termIn);
			return this;
		}
		
		public Builder onlyBefore(Supplier<Term>... term)
		{
			for(Supplier<Term> termIn : term)
				before.add(termIn);
			return this;
		}
		
		public Builder neverBefore(Supplier<Term>... term)
		{
			for(Supplier<Term> termIn : term)
				notBefore.add(termIn);
			return this;
		}
		
		public Builder onApply(TermApplyFunc funcIn)
		{
			applyFunc = funcIn;
			return this;
		}
		
		public Term build(Identifier registryName)
		{
			return new Term(registryName, placeable, replaceable, groups)
				{
					public boolean canBePlaced(Room inRoom, List<Room> previous, List<Room> next, Graph graph)
					{
						// Only placeable whilst graph is below a maximum scale
						if(sizeCap > 0 && graph.size() >= sizeCap)
							return false;
						
						// Only placeable after a minimum number of preceding rooms
						if(depthMin > 0 && inRoom.depth <= depthMin)
							return false;
						
						// Only placeable up to a maximum population in the same graph
						if(maxPop > 0 && graph.tally(this) >= maxPop)
							return false;
						
						// Cannot be placed at a dead end
						if(!deadEnds && !inRoom.hasLinks())
							return false;
						
						final Predicate<Term> prevCheck = t -> Term.checkListFor(previous, t);
						final Predicate<Term> nextCheck = t -> Term.checkListFor(next, t);
						
						// Cannot be placed consecutively
						if(!afterSelf && (prevCheck.test(this) || nextCheck.test(this)))
							return false;
						
						// Must be placed after one or more possible rooms
						if(!after.isEmpty() && terms(after).noneMatch(prevCheck::test))
							return false;
						
						// Must not be placed after one or more possible rooms
						if(!notAfter.isEmpty() && terms(notAfter).anyMatch(prevCheck::test))
							return false;
						
						if(!next.isEmpty())
						{
							// Must be placed before one or more possible rooms
							if(!before.isEmpty() && terms(before).noneMatch(nextCheck::test))
								return false;
							
							// Must not be placed before one or more possible rooms
							if(!notBefore.isEmpty() && terms(notBefore).anyMatch(nextCheck::test))
								return false;
						}
						
						return true;
					}
					
					private static Stream<Term> terms(List<Supplier<Term>> setIn)
					{
						return setIn.stream().map(Supplier::get).filter(Objects::nonNull);
					}
					
					public void onApply(Room room, Graph graph) { applyFunc.apply(this, room, graph); }
				};
		}
		
		@FunctionalInterface
		public interface TermApplyFunc
		{
			public void apply(Term term, Room room, Graph graph);
		}
	}
}