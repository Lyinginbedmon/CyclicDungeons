package com.lying.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.Line2f;
import com.lying.utility.RotaryBox2f;
import com.lying.worldgen.Tile;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;

public class BlueprintPassage
{
	public static final Map<Tile,Float> PASSAGE_TILE_SET = Map.of(
			CDTiles.FLOOR.get(), 10000F,
			CDTiles.AIR.get(), 10F
			);
	
	public static final int PASSAGE_WIDTH = 3;
	private final BlueprintRoom parent, child;
	private final Line2f line;
	private final CompoundBox2f box;
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b)
	{
		this(a, b, PASSAGE_WIDTH);
	}

	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b, float width)
	{
		this(a, b, new Line2f(new Vec2f(a.position().x, a.position().y), new Vec2f(b.position().x, b.position().y)), width);
	}
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b, Line2f lineIn, float width)
	{
		parent = a;
		child = b;
		line = lineIn;
		box = lineToBox(asLines(), width);
	}
	
	public BlueprintRoom parent() { return parent; }
	
	public BlueprintRoom child() { return child; }
	
	/** Returns this passage as a singular line */
	public Line2f asLine() { return line; }
	
	/** Returns this passage as a set of one or more line segments */
	public List<Line2f> asLines()
	{		
		return LineUtils.trialLines(line.getLeft(), line.getRight(), this::isLineViable);
	}
	
	/** Controls how this passage is shaped to connect its associated rooms */
	private boolean isLineViable(List<Line2f> line)
	{
		// Terminal bounding boxes (expanded slightly to encourage spacing)
		AbstractBox2f firstB = parent.bounds().grow(1F);
		AbstractBox2f lastB = child.bounds().grow(1F);
		
		// Terminal line segments
		Line2f firstL = line.getFirst();
		Line2f lastL = line.getLast();
		
		// Terminal line segments must not be entirely contained within the terminal bounding boxes
		final Predicate<Line2f> firstContains = l -> firstB.contains(l.getLeft()) && firstB.contains(l.getRight());
		final Predicate<Line2f> lastContains = l -> lastB.contains(l.getLeft()) && lastB.contains(l.getRight());
		if(
				firstContains.test(firstL) || firstContains.test(lastL) ||
				lastContains.test(firstL) || lastContains.test(lastL))
			return false;
		
		// No subsequent line segment bounds may intersect the terminal bounding boxes
		if(
				line.subList(1, line.size()).stream().anyMatch(firstB::intersects) || 
				line.subList(0, line.size() - 1).stream().anyMatch(lastB::intersects))
			return false;
		
		if(
				firstB.intersects(lineToBox(line.subList(1, line.size()), PASSAGE_WIDTH)) || 
				lastB.intersects(lineToBox(line.subList(0, line.size()-1), PASSAGE_WIDTH)))
			return false;
		
		return true;
	}
	
	public AbstractBox2f asBox() { return box; }
	
	protected static CompoundBox2f lineToBox(List<Line2f> segments, float width)
	{
		CompoundBox2f box = new CompoundBox2f();
		for(Line2f l : segments)
			box.add(RotaryBox2f.fromLine(l, width));
		
		return box;
	}
	
	public void build(BlockPos origin, ServerWorld world, List<AbstractBox2f> boundaries)
	{
		// FIXME Calculate tile set and generate with WFC
		
		for(Line2f segment : asLines())
		{
			Vec2f offset = segment.getRight().add(segment.getLeft().negate());
			float len = offset.length();
			offset = offset.normalize();
			for(int i=0; i<len; i++)
			{
				Vec2f point = segment.getLeft().add(offset.multiply(i));
				BlockPos pos = origin.add((int)point.x, 0, (int)point.y);
				Tile.tryPlace(Blocks.GRAY_CONCRETE_POWDER.getDefaultState(), pos, world);
			}
		}
	}
	
	/** Returns true if the given point is either end of this passage */
	public boolean isTerminus(Vec2f point)
	{
		return line.getLeft().equals(point) || line.getRight().equals(point);
	}
	
	/** Returns true if the given room is either intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		Vector2i pos = room.position();
		return isTerminus(new Vec2f(pos.x, pos.y));
	}
	
	public boolean isTerminus(BlueprintPassage b) { return isTerminus(b.line.getLeft()) || isTerminus(b.line.getRight()); }
	
	public boolean intersects(BlueprintPassage b)
	{
		if(canShareSpaceWith(b))
		{
			// Allow intersection if both passages start from the same parent
			// This promotes the generation of junctions and reduces overall doorway counts
			return false;
		}
		
		List<Line2f> linesA = asLines();
		List<Line2f> linesB = b.asLines();
		return linesA.stream().anyMatch(l -> linesB.stream().anyMatch(l::intersects));
	}
	
	public boolean canShareSpaceWith(BlueprintPassage b)
	{
		return b.parent.equals(parent) && b.child.metadata().depth() == child.metadata().depth();
	}
	
	/** Returns true if this passage intersects with any other passages in the given chart */
	public boolean hasIntersections(List<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Blueprint.getPassages(chart);
		return paths.stream()
				.filter(p -> !isTerminus(p))
				.anyMatch(this::intersects);
	}
	
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean hasTunnels(List<BlueprintRoom> chart)
	{
		return chart.stream()
				.filter(r -> !isTerminus(r))
				.map(BlueprintRoom::bounds)
				.anyMatch(this.box::intersects);
	}
	
	private static class LineUtils
	{
		private static final List<BiFunction<Vec2f,Vec2f,ArrayList<Line2f>>> providers = List.of(
				LineUtils::xFirstCurved,
				LineUtils::yFirstCurved,
				LineUtils::xFirst,
				LineUtils::yFirst,
				LineUtils::diagonal
				);
		
		/** Attempts to generate a viable deterministic line, from the most elegant to the least */
		public static List<Line2f> trialLines(Vec2f start, Vec2f end, Predicate<List<Line2f>> qualifier)
		{
			ArrayList<Line2f> line = Lists.newArrayList();
			for(BiFunction<Vec2f,Vec2f,ArrayList<Line2f>> provider : providers)
			{
				line = provider.apply(start, end);
				line.removeIf(l -> l.length() == 0F);
				line.removeIf(Objects::isNull);
				
				if(line.isEmpty())
					continue;
				
				if(line.size() == 1 || qualifier.test(line))
					return line;
			}
			
			return line;
		}
		
		private static ArrayList<Line2f> diagonal(Vec2f start, Vec2f end)
		{
			return Lists.newArrayList(new Line2f(start, end));
		}
		
		private static ArrayList<Line2f> xFirst(Vec2f start, Vec2f end)
		{
			Vec2f offset = end.add(start.negate());
			Vec2f mid = start.add(new Vec2f(offset.x, 0F));
			return Lists.newArrayList(
					new Line2f(start, mid),
					new Line2f(mid, end)
					);
		}
		
		private static ArrayList<Line2f> yFirst(Vec2f start, Vec2f end)
		{
			Vec2f offset = end.add(start.negate());
			Vec2f mid = start.add(new Vec2f(0F, offset.y));
			return Lists.newArrayList(
					new Line2f(start, mid),
					new Line2f(mid, end)
					);
		}
		
		private static ArrayList<Line2f> xFirstCurved(Vec2f start, Vec2f end)
		{
			Line2f direct = new Line2f(start, end);
			if(Math.abs(direct.m) != 1)
				return Lists.newArrayList();
			
			ArrayList<Line2f> lines = Lists.newArrayList();
			
			Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
			Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
			
			Vec2f a = start.add(toX);
			Vec2f b = end.add(toY.negate());
			lines.add(new Line2f(start, a));
			lines.add(new Line2f(a, b));
			lines.add(new Line2f(b, end));
			return lines;
		}
		
		private static ArrayList<Line2f> yFirstCurved(Vec2f start, Vec2f end)
		{
			Line2f direct = new Line2f(start, end);
			if(Math.abs(direct.m) != 1)
				return Lists.newArrayList();
			
			ArrayList<Line2f> lines = Lists.newArrayList();
			
			Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
			Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
			
			Vec2f a = start.add(toY);
			Vec2f b = end.add(toX.negate());
			lines.add(new Line2f(start, a));
			lines.add(new Line2f(a, b));
			lines.add(new Line2f(b, end));
			return lines;
		}
	}
}
