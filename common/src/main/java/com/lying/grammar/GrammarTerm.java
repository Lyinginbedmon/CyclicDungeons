package com.lying.grammar;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDLoggers;
import com.lying.init.CDTerms;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.DebugLogger;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.Tile;
import com.lying.worldgen.TileGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;

public abstract class GrammarTerm
{
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	protected static final Codec<GrammarTerm> CODEC = Identifier.CODEC.comapFlatMap(id -> 
	{
		Optional<GrammarTerm> type = CDTerms.get(id);
		if(type.isPresent())
			return DataResult.success(type.get());
		else
			return DataResult.error(() -> "Not a recognised type: '"+String.valueOf(id) + "'");
	}, GrammarTerm::registryName);
	
	// TODO Give each term its own weight map
	private static final Map<Tile, Float> BASIC_TILE_SET = Map.of(
			CDTiles.FLOOR.get(), 10000F,
			CDTiles.PUDDLE.get(), 1000F,
			CDTiles.POOL.get(), 100F,
			CDTiles.AIR.get(), 10F,
			CDTiles.SEAT.get(), 10F,
			CDTiles.FLOOR_LIGHT.get(), 1F,
			CDTiles.TABLE.get(), 1F,
			CDTiles.TABLE_LIGHT.get(), 1F,
			CDTiles.WORKSTATION.get(), 1F
			);
	
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
	
	public boolean generate(BlockPos position, ServerWorld world, BlueprintRoom node, List<BlueprintPassage> passages)
	{
		BlueprintTileGrid map = BlueprintTileGrid.fromGraphGrid(node.tileGrid(), Blueprint.ROOM_TILE_HEIGHT);
		
		// Pre-seed doorways to connecting rooms
//		preseedDoorways(position, min, node, map, passages);	FIXME Update passage preseeding
		
		// Fill rest of tileset with WFC generation
		TileGenerator.generate(map, BASIC_TILE_SET, world.getRandom());
		
		map.finalise();
		return map.generate(position, world);
	}
	
	protected static void preseedDoorways(BlockPos position, BlockPos min, BlueprintRoom node, BlueprintTileGrid map, List<BlueprintPassage> passages)
	{
		List<BlueprintPassage> doorways = passages.stream().filter(p -> p.isTerminus(node)).toList();
		
		// Find all line segments that intersect the bounds of the room
		AbstractBox2f bounds = node.tileBounds();
		List<LineSegment2f> lines = Lists.newArrayList();
		doorways.stream()
			.map(BlueprintPassage::asLines)
			.forEach(set -> 
				set.stream()
				.filter(bounds::intersects)
				.forEach(lines::add));
		
		// Offset line segments to world coordinates
		Vec2f toWorld = new Vec2f(position.getX(), position.getZ());
		Vec2f toRoom = new Vec2f(min.getX(), min.getZ()).negate();
		lines.stream()
			// Convert graph to world space
			.map(v -> v.offset(toWorld))
			// Convert world space to room space
			.map(v -> v.offset(toRoom))
			.forEach(l -> 
			{
				// Segment points in room local coordinates
				Vec2f startV = l.getLeft();
				Vec2f endV = l.getRight();
				
				// March between points until intersecting boundary
				Vec2f offset = endV.add(startV.negate());
				float len = offset.length();
				offset = offset.normalize();
				for(int i=0; i<len; i++)
				{
					Vec2f point = startV.add(offset.multiply(i));
					BlockPos tile = new BlockPos((int)(point.x / Tile.TILE_SIZE), 1, (int)(point.y / Tile.TILE_SIZE));
					if(map.contains(tile) && Direction.Type.HORIZONTAL.stream().anyMatch(d -> map.isBoundary(tile, d)))
					{
						map.put(tile, CDTiles.PASSAGE.get());
						break;
					}
				}
			});
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