package com.lying.grammar;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public abstract class Term
{
	private final Identifier registryName;
	private final boolean isReplaceable, isPlaceable;
	
	private Term(Identifier idIn, boolean placeable, boolean replaceable)
	{
		registryName = idIn;
		isPlaceable = placeable;
		isReplaceable = replaceable;
	}
	
	public Identifier registryName() { return registryName; }
	
	public MutableText name() { return Text.literal(registryName.getPath()); }
	
	public boolean matches(Term b) { return registryName.equals(b.registryName); }
	
	/** Returns true if generation should replace rooms with this term */
	public boolean isReplaceable() { return isReplaceable; }
	
	/** Returns true if generation can place this kind of room */
	public boolean isPlaceable() { return isPlaceable; }
	
	/** Returns true if this Term can exist in the given room */
	public abstract boolean canBePlaced(Room inRoom, Room previous, @Nullable Room next, Graph graph);
	
	public void applyTo(Room room, Graph graph)
	{
		room.setTerm(this);
		onApply(room, graph);
	}
	
	protected abstract void onApply(Room room, Graph graph);
	
	protected static Room injectRoom(Room room, Graph graph)
	{
		Room injected = new Room(UUID.randomUUID());
		room.getLink().ifPresent(injected::linkTo);
		room.linkTo(injected.uuid());
		graph.add(injected);
		return injected;
	}
	
	public static class Builder
	{
		private boolean replaceable = false;
		private boolean placeable = true;
		private boolean afterSelf = true;
		private int maxPop = -1, sizeCap = -1;
		private int depthMin = -1;
		private List<Supplier<Term>> after = Lists.newArrayList(), before = Lists.newArrayList();
		private List<Supplier<Term>> notAfter = Lists.newArrayList(), notBefore = Lists.newArrayList();
		private TermApplyFunc applyFunc = (t,r,g) -> {};
		
		private Builder() { }
		
		public static Builder create()
		{
			return new Builder();
		}
		
		public Builder system()
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
		
		@SuppressWarnings("unchecked")
		public Builder onlyAfter(Supplier<Term>... term)
		{
			for(Supplier<Term> termIn : term)
				after.add(termIn);
			return this;
		}
		
		public Builder neverAfter(Supplier<Term> term)
		{
			notAfter.add(term);
			return this;
		}
		
		public Builder onlyBefore(Supplier<Term> term)
		{
			before.add(term);
			return this;
		}
		
		public Builder neverBefore(Supplier<Term> term)
		{
			notBefore.add(term);
			return this;
		}
		
		public Builder onApply(TermApplyFunc funcIn)
		{
			applyFunc = funcIn;
			return this;
		}
		
		public Term build(Identifier registryName)
		{
			return new Term(registryName, placeable, replaceable)
				{
					public boolean canBePlaced(Room inRoom, Room previous, @Nullable Room next, Graph graph)
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
						
						// Cannot be placed consecutively
						if(!afterSelf && (previous.is(this) || next.is(this)))
							return false;
						
						// Must be placed after one or more possible rooms
						if(!after.isEmpty() && after.stream().map(Supplier::get).noneMatch(previous::is))
							return false;
						
						// Must not be placed after one or more possible rooms
						if(!notAfter.isEmpty() && notAfter.stream().map(Supplier::get).anyMatch(previous::is))
							return false;
						
						if(next != null)
						{
							// Must be placed before one or more possible rooms
							if(!before.isEmpty() && before.stream().map(Supplier::get).noneMatch(next::is))
								return false;
							
							// Must not be placed before one or more possible rooms
							if(!notBefore.isEmpty() && notBefore.stream().map(Supplier::get).anyMatch(next::is))
								return false;
						}
						
						return true;
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