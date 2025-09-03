package com.lying.grammar;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public abstract class GrammarTerm
{
	protected static final Codec<GrammarTerm> CODEC = Identifier.CODEC.comapFlatMap(id -> 
	{
		Optional<GrammarTerm> type = CDTerms.get(id);
		if(type.isPresent())
			return DataResult.success(type.get());
		else
			return DataResult.error(() -> "Not a recognised type: '"+String.valueOf(id) + "'");
	}, GrammarTerm::registryName);
	
	private final Identifier registryName;
	private final int colour;
	private final int weight;
	private final boolean isReplaceable, isPlaceable, isBranchInjector;
	
	private GrammarTerm(Identifier idIn, int weightIn, int colourIn, boolean placeable, boolean replaceable, boolean injectsBranch)
	{
		registryName = idIn;
		weight = weightIn;
		colour = colourIn;
		isPlaceable = placeable;
		isReplaceable = replaceable;
		isBranchInjector = injectsBranch;
	}
	
	public final Identifier registryName() { return registryName; }
	
	public final int colour() { return colour; }
	
	public final int weight() { return weight; }
	
	public MutableText name() { return Text.literal(registryName.getPath()); }
	
	public boolean matches(GrammarTerm b) { return registryName.equals(b.registryName); }
	
	/** Returns true if generation should replace rooms with this term */
	public boolean isReplaceable() { return isReplaceable; }
	
	/** Returns true if generation can place this kind of room */
	public boolean isPlaceable() { return isPlaceable; }
	
	/** Returns true if this term adds a new branch to the graph */
	public boolean isBranchInjector() { return isBranchInjector; }
	
	/** Returns true if this Term can exist in the given room */
	public abstract boolean canBePlaced(CDRoom inRoom, @NotNull List<CDRoom> previous, @NotNull List<CDRoom> next, CDGraph graph);
	
	public void applyTo(CDRoom room, CDGraph graph)
	{
		room.metadata().setType(this);
		onApply(room, graph);
	}
	
	protected abstract void onApply(CDRoom room, CDGraph graph);
	
	public static CDRoom injectRoom(CDRoom room, CDGraph graph)
	{
		CDRoom injected = new CDRoom();
		room.getChildLinks().forEach(uuid -> 
		{
			Optional<CDRoom> child = graph.get(uuid);
			if(child.isEmpty())
				return;
			
			// Move all links of parent to child
			injected.linkTo(child.get());
			room.detachFrom(child.get());
		});
		// Link parent to child and add to graph
		room.linkTo(injected);
		graph.add(injected);
		return injected;
	}
	
	public static CDRoom injectBranch(CDRoom room, CDGraph graph)
	{
		CDRoom injected = new CDRoom();
		room.linkTo(injected);
		graph.add(injected);
		return injected;
	}
	
	protected static boolean checkListFor(@Nullable List<CDRoom> rooms, GrammarTerm term)
	{
		return rooms != null && !rooms.isEmpty() && rooms.stream().filter(Objects::nonNull).anyMatch(r -> r.metadata().is(term));
	}
	
	@SuppressWarnings("unchecked")
	public static class Builder
	{
		private final int colour;
		private int weight = 1;
		private boolean replaceable = false;
		private boolean placeable = true;
		private boolean afterSelf = true;
		private boolean deadEnds = true;
		private boolean injects = false;
		private int maxPop = -1, sizeCap = -1;
		private int depthMin = -1;
		private List<Supplier<GrammarTerm>> after = Lists.newArrayList(), before = Lists.newArrayList();
		private List<Supplier<GrammarTerm>> notAfter = Lists.newArrayList(), notBefore = Lists.newArrayList();
		private TermApplyFunc applyFunc = (t,r,g) -> {};
		
		private Builder(int colourIn)
		{
			colour = colourIn;
		}
		
		public static Builder create(int colour)
		{
			return new Builder(colour);
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
		
		public Builder injectsBranches()
		{
			injects = true;
			return this;
		}
		
		public Builder weight(int val)
		{
			weight = val;
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
		
		public Builder onlyAfter(Supplier<GrammarTerm>... term)
		{
			for(Supplier<GrammarTerm> termIn : term)
				after.add(termIn);
			return this;
		}
		
		public Builder neverAfter(Supplier<GrammarTerm>... term)
		{
			for(Supplier<GrammarTerm> termIn : term)
				notAfter.add(termIn);
			return this;
		}
		
		public Builder onlyBefore(Supplier<GrammarTerm>... term)
		{
			for(Supplier<GrammarTerm> termIn : term)
				before.add(termIn);
			return this;
		}
		
		public Builder neverBefore(Supplier<GrammarTerm>... term)
		{
			for(Supplier<GrammarTerm> termIn : term)
				notBefore.add(termIn);
			return this;
		}
		
		public Builder onApply(TermApplyFunc funcIn)
		{
			applyFunc = funcIn;
			return this;
		}
		
		public GrammarTerm build(Identifier registryName)
		{
			return new GrammarTerm(registryName, weight, colour, placeable, replaceable, injects)
				{
					public boolean canBePlaced(CDRoom inRoom, List<CDRoom> previous, List<CDRoom> next, CDGraph graph)
					{
						// Only placeable whilst graph is below a maximum scale
						if(sizeCap > 0 && graph.size() >= sizeCap)
							return false;
						
						// Only placeable after a minimum number of preceding rooms
						if(depthMin > 0 && inRoom.metadata().depth() <= depthMin)
							return false;
						
						// Only placeable up to a maximum population in the same graph
						if(maxPop > 0 && graph.tally(this) >= maxPop)
							return false;
						
						// Cannot be placed at a dead end
						if(!deadEnds && !inRoom.hasLinks())
							return false;
						
						// Cannot be applied to a room with the maximum number of connections
						if(injects && !inRoom.canAddLink())
							return false;
						
						final Predicate<GrammarTerm> prevCheck = t -> GrammarTerm.checkListFor(previous, t);
						final Predicate<GrammarTerm> nextCheck = t -> GrammarTerm.checkListFor(next, t);
						
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
					
					private static Stream<GrammarTerm> terms(List<Supplier<GrammarTerm>> setIn)
					{
						return setIn.stream().map(Supplier::get).filter(Objects::nonNull);
					}
					
					public void onApply(CDRoom room, CDGraph graph) { applyFunc.apply(this, room, graph); }
				};
		}
		
		@FunctionalInterface
		public interface TermApplyFunc
		{
			public void apply(GrammarTerm term, CDRoom room, CDGraph graph);
		}
	}
}