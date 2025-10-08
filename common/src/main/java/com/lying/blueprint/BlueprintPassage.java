package com.lying.blueprint;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.Line2f;
import com.lying.utility.LineUtils;
import com.lying.utility.RotaryBox2f;
import com.lying.worldgen.Tile;

import net.minecraft.block.BlockState;
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
	private final BlueprintRoom parent;
	private final List<BlueprintRoom> children = Lists.newArrayList();
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
		children.add(b);
		line = lineIn;
		box = lineToBox(asLines(), width);
	}
	
	public BlueprintRoom parent() { return parent; }
	
	public List<BlueprintRoom> children() { return children; }
	
	public BlueprintPassage addChild(BlueprintRoom room)
	{
		if(children.stream().noneMatch(room::equals))
			children.add(room);
		return this;
	}
	
	public int size() { return 1 + children.size(); }
	
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
		List<BlueprintRoom> rooms = Lists.newArrayList();
		rooms.add(parent);
		rooms.addAll(children);
		return isLineViable(line, rooms);
	}
	
	public static boolean isLineViable(List<Line2f> line, List<BlueprintRoom> rooms)
	{
//		Map<Line2f, List<AbstractBox2f>> terminalMap = new HashMap<>();
//		line.forEach(l -> 
//		{
//			List<AbstractBox2f> terminals = rooms.stream().filter(r -> 
//			{
//				Vector2i pos = r.position();
//				return l.isEitherPoint(new Vec2f(pos.x, pos.y));
//			}).map(BlueprintRoom::bounds).map(b -> b.grow(1F)).toList();
//			if(terminals.isEmpty())
//				return;
//			
//			terminalMap.put(l, terminals);
//		});
//		
//		List<Line2f> exteriorLines = Lists.newArrayList();
//		line.stream().filter(l -> rooms.stream().map(BlueprintRoom::bounds).map(b -> b.grow(1F)).noneMatch(b -> b.intersects(l))).forEach(exteriorLines::add);
//		
//		// The sum of all terminal line segments and all external line segments must be equal to the total number of line segments
//		if(terminalMap.size() + exteriorLines.size() != line.size())
//			return false;
//
//		// Terminal line segments must not be entirely contained within the terminal bounding boxes
//		if(terminalMap.entrySet().stream().anyMatch(e -> e.getValue().stream().anyMatch(b -> 
//		{
//			Line2f l = e.getKey();
//			return b.contains(l.getLeft()) && b.contains(l.getRight());
//		})))
//			return false;
		
		AbstractBox2f firstB = rooms.getFirst().bounds().grow(1F);
		AbstractBox2f lastB = rooms.getLast().bounds().grow(1F);
		
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
	
	/** Returns true if the given point is either end of this passage */
	public boolean isTerminus(Vec2f point)
	{
		Vector2i vec = new Vector2i((int)point.x, (int)point.y);
		return parent.position().equals(vec) || children.stream().map(BlueprintRoom::position).anyMatch(vec::equals);
	}
	
	/** Returns true if the given room is either intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		return room.equals(parent) || children.stream().anyMatch(room::equals);
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
		Predicate<RoomMetadata> depthMatch = m -> m.depth() == children.get(0).metadata().depth();
		return b.parent.equals(parent) && b.children.stream().map(BlueprintRoom::metadata).allMatch(depthMatch);
	}
	
	public boolean canMergeWith(BlueprintPassage b)
	{
		if(!canShareSpaceWith(b))
			return false;
		
		List<Line2f> linesA = asLines();
		List<Line2f> linesB = b.asLines();
		return linesA.stream().anyMatch(l -> linesB.stream().anyMatch(l::intersects));
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
	
	public void build(BlockPos origin, ServerWorld world, List<AbstractBox2f> boundaries, BlockState placeState)
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
				Tile.tryPlace(placeState, pos, world);
				Tile.tryPlace(placeState, pos.up(), world);
			}
		}
	}
}
