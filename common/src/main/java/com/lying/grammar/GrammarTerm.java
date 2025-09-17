package com.lying.grammar;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintRoom;
import com.lying.init.CDTerms;
import com.lying.utility.CDUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

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
	private final DyeColor color;
	private final int weight;
	private final boolean isReplaceable, isPlaceable, isBranchInjector;
	private final TermConditions conditions;
	private final Function<Random, Vector2i> sizeFunc;
	
	private GrammarTerm(
			Identifier idIn, 
			int weightIn, 
			int colourIn, 
			DyeColor colorIn, 
			Function<Random, Vector2i> sizeFuncIn, 
			boolean placeable, 
			boolean replaceable, 
			boolean injectsBranch,
			TermConditions conditionsIn)
	{
		registryName = idIn;
		weight = weightIn;
		colour = colourIn;
		color = colorIn;
		sizeFunc = sizeFuncIn;
		conditions = conditionsIn;
		isPlaceable = placeable;
		isReplaceable = replaceable;
		isBranchInjector = injectsBranch;
	}
	
	public final Identifier registryName() { return registryName; }
	
	public final int colour() { return colour; }
	
	public final DyeColor color() { return color; }
	
	public final int weight() { return weight; }
	
	public MutableText name() { return Text.literal(registryName.getPath()); }
	
	public boolean matches(GrammarTerm b) { return registryName.equals(b.registryName); }
	
	/** Returns true if generation should replace rooms with this Term */
	public boolean isReplaceable() { return isReplaceable; }
	
	/** Returns true if generation can place this kind of room */
	public boolean isPlaceable() { return isPlaceable; }
	
	/** Returns true if this Term adds a new branch to the graph */
	public boolean isBranchInjector() { return isBranchInjector; }
	
	/** Returns true if this Term can exist in the given room */
	public final boolean canBePlaced(GrammarRoom inRoom, @NotNull List<GrammarRoom> previous, @NotNull List<GrammarRoom> next, GrammarPhrase graph)
	{
		return (!isBranchInjector() || inRoom.canAddLink()) && conditions.test(this, inRoom, previous, next, graph);
	}
	
	public boolean generate(BlockPos min, BlockPos max, ServerWorld world, BlueprintRoom node, Blueprint chart)
	{
		// Build exterior walls
		final BlockState wall = Blocks.DEEPSLATE_BRICKS.getDefaultState();
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
		{
			if((p.getX() == min.getX() || p.getX() == max.getX()) || (p.getZ() == min.getZ() || p.getZ() == max.getZ()))
			{
				Blueprint.tryPlaceAt(wall, p, world);
				for(int i=2; i>0; i--)
					Blueprint.tryPlaceAt(wall, p.up(i), world);
			}
		});
		
		// Lay flooring
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
		{
			if(p.getX() == min.getX() || p.getX() == max.getX())
				return;
			else if(p.getZ() == min.getZ() || p.getZ() == max.getZ())
				return;
			
			DyeColor color = node.metadata().type().color();
			Blueprint.tryPlaceAt(CDUtils.dyeToConcretePowder(color).getDefaultState(), p, world);
		});
		
		// Draw connecting paths
		List<BlueprintRoom> pathsTo = Lists.newArrayList();
		pathsTo.addAll(node.getChildren(chart));
		pathsTo.addAll(node.getParents(chart));
		final BlockPos start = min.add(node.metadata().size().x() / 2, 0, node.metadata().size().y() / 2);
		final BlockPos offset = start.subtract(new BlockPos(node.position().x(), 0, node.position().y()));
		pathsTo.stream().map(BlueprintRoom::position).forEach(end -> 
		{
			BlockPos pos2 = offset.add(end.x(), 0, end.y());
			BlockPos current = start;
			while(current.getSquaredDistance(pos2) > 0 && Box.enclosing(min, max).contains(current.toCenterPos()))
			{
				double minDist = Double.MAX_VALUE;
				Direction face = Direction.NORTH;
				for(Direction facing : Direction.Type.HORIZONTAL)
				{
					double dist = current.offset(facing).getSquaredDistance(pos2);
					if(minDist > dist)
					{
						face = facing;
						minDist = dist;
					}
				}
				
				Blueprint.tryPlaceAt(Blocks.SMOOTH_STONE.getDefaultState(), current, world);
				for(int i=2; i>0; i--)
					Blueprint.tryPlaceAt(Blocks.AIR.getDefaultState(), current.up(i), world);
				current = current.offset(face);
			}
		});
		
		return true;
	}
	
	public void applyTo(GrammarRoom room, GrammarPhrase graph)
	{
		room.metadata().setType(this);
		onApply(room, graph);
	}
	
	protected abstract void onApply(GrammarRoom room, GrammarPhrase graph);
	
	public Vector2i size(Random rand) { return sizeFunc.apply(rand); }
	
	public static GrammarRoom injectRoom(GrammarRoom room, GrammarPhrase graph)
	{
		GrammarRoom injected = new GrammarRoom();
		room.getChildLinks().forEach(uuid -> 
		{
			Optional<GrammarRoom> child = graph.get(uuid);
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
	
	public static GrammarRoom injectBranch(GrammarRoom room, GrammarPhrase graph)
	{
		GrammarRoom injected = new GrammarRoom();
		room.linkTo(injected);
		graph.add(injected);
		return injected;
	}
	
	protected static boolean checkListFor(@Nullable List<GrammarRoom> rooms, GrammarTerm term)
	{
		return rooms != null && !rooms.isEmpty() && rooms.stream().filter(Objects::nonNull).anyMatch(r -> r.metadata().is(term));
	}
	
	@SuppressWarnings("unchecked")
	public static class Builder
	{
		private final int colour;
		private final DyeColor color;
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
		private Function<Random, Vector2i> sizeFunc = r -> new Vector2i(3 + r.nextInt(4), 3 + r.nextInt(4));
		
		private Builder(int colourIn, DyeColor colorIn)
		{
			colour = colourIn;
			color = colorIn;
		}
		
		public static Builder create(int colour, DyeColor color)
		{
			return new Builder(colour, color);
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
		
		public Builder size(Function<Random, Vector2i> func)
		{
			sizeFunc = func;
			return this;
		}
		
		public Builder size(Vector2i vec)
		{
			sizeFunc = r -> vec;
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
			TermConditions conditions = TermConditions.create()
					.nonconsecutive(afterSelf)
					.sizeCap(sizeCap)
					.afterDepth(depthMin)
					.popCap(maxPop)
					.allowDeadEnds(deadEnds)
					.onlyAfter(after).neverAfter(notAfter)
					.onlyBefore(before).neverBefore(notBefore);
			
			return new GrammarTerm(registryName, weight, colour, color, sizeFunc, placeable, replaceable, injects, conditions)
				{
					public void onApply(GrammarRoom room, GrammarPhrase graph) { applyFunc.apply(this, room, graph); }
				};
		}
		
		@FunctionalInterface
		public interface TermApplyFunc
		{
			public void apply(GrammarTerm term, GrammarRoom room, GrammarPhrase graph);
		}
	}
}