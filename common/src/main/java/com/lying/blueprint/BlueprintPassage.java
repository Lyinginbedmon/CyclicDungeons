package com.lying.blueprint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.utility.LineUtils;
import com.lying.utility.RotaryBox2f;
import com.lying.utility.Vector2iUtils;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Vec2f;

public class BlueprintPassage
{
	public static final Map<Tile,Float> PASSAGE_TILE_SET = Map.of(
			CDTiles.FLOOR.get(), 10000F,
			CDTiles.AIR.get(), 10F
			);
	
	public static final int PASSAGE_WIDTH = 3 * Tile.TILE_SIZE;
	private final BlueprintRoom parent;
	private final List<BlueprintRoom> children = Lists.newArrayList();
	private final float width;
	private CompoundBox2f box;
	
	private List<LineSegment2f> linesCached = Lists.newArrayList();
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b)
	{
		this(a, b, PASSAGE_WIDTH);
	}
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b, float width)
	{
		this(a, b, new LineSegment2f(a.position(), b.position()), width);
	}
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b, LineSegment2f lineIn, float widthIn)
	{
		parent = a;
		children.add(b);
		width = widthIn;
		box = lineToBox(asLines(), width);
	}
	
	public BlueprintRoom parent() { return parent; }
	
	public List<BlueprintRoom> children() { return children; }
	
	public BlueprintPassage addChild(BlueprintRoom room)
	{
		if(children.stream().noneMatch(room::equals))
		{
			children.add(room);
			box = lineToBox(asLines(), width);
		}
		return this;
	}
	
	public int size() { return 1 + children.size(); }
	
	public int lineSegments() { return asLines().size(); }
	
	public double length()
	{
		double len = 0D;
		for(LineSegment2f line : asLines())
			len += line.length();
		return len;
	}
	
	/** Returns this passage as a set of one or more line segments */
	public List<LineSegment2f> asLines()
	{
		if(linesCached.isEmpty())
			cacheLines();
		
		return linesCached;
	}
	
	public void cacheLines()
	{
		linesCached.clear();
		
		List<LineSegment2f> lines = Lists.newArrayList();
		children.stream()
			.map(p -> LineUtils.trialLines(parent, p, this::isLineViable))
			.forEach(lines::addAll);
		
		if(linesCached.isEmpty())
			linesCached.addAll(lines);
	}
	
	public static List<Vec2f> asPoints(List<LineSegment2f> lines)
	{
		List<Vec2f> points = Lists.newArrayList();
		lines.stream().forEach(l -> 
		{
			Vec2f left = l.getLeft();
			if(!points.contains(left))
				points.add(left);
			
			Vec2f right = l.getRight();
			if(!points.contains(right))
				points.add(right);
		});
		return points;
	}
	
	/** Subtracts the given bounding box from all lines in this passage */
	public BlueprintPassage exclude(AbstractBox2f box)
	{
		if(linesCached.isEmpty())
		{
			asLines();
			if(linesCached.isEmpty())
				return this;
		}
		
		// Remove any lines that exist wholly within the bounding box
		linesCached.removeIf(box::contains);
		
		List<LineSegment2f> clipped = Lists.newArrayList();
		linesCached.stream().map(l -> l.clip(box)).filter(Objects::nonNull).forEach(clipped::add);
		
		linesCached.clear();
		linesCached.addAll(clipped);
		
		return this;
	}
	
	/** Controls how this passage is shaped to connect its associated rooms */
	private boolean isLineViable(List<LineSegment2f> line)
	{
		List<BlueprintRoom> rooms = Lists.newArrayList();
		rooms.add(parent);
		rooms.addAll(children);
		return isLineViable(line, rooms);
	}
	
	public static boolean isLineViable(List<LineSegment2f> segments, List<BlueprintRoom> rooms)
	{
		if(segments.size() == 1)
			return true;
		
		final List<AbstractBox2f> bounds = BlueprintOrganiser.getBounds(rooms);
		
		// Map of line segments that start or end within a terminal bounding box
		Map<LineSegment2f, List<AbstractBox2f>> terminalMap = new HashMap<>();
		segments.forEach(l -> 
		{
			List<AbstractBox2f> terminals = rooms.stream()
				.filter(r -> l.contains(Vector2iUtils.toVec2f(r.position())))
				.map(BlueprintRoom::bounds)
				.map(b -> b.grow(1F)).toList();
			
			if(!terminals.isEmpty())
				terminalMap.put(l, terminals);
		});
		
		// Terminal line segments must not be entirely contained within any single terminal bounding box
		if(terminalMap.entrySet().stream().anyMatch(e -> e.getValue().stream().anyMatch(b -> 
		{
			LineSegment2f l = e.getKey();
			return b.contains(l.getLeft()) && b.contains(l.getRight());
		})))
			return false;
		
		// List of line segments that do not intersect any terminal bounding boxes
		List<LineSegment2f> exteriorLines = segments.stream()
				.filter(l -> 
				{
					AbstractBox2f box = RotaryBox2f.fromLine(l, PASSAGE_WIDTH);
					return bounds.stream()
						.map(b -> b.grow(1F))
						.noneMatch(Predicates.not(box::intersects)); 
				}).toList();
		
		// The sum of all terminal line segments and all external line segments must be equal to the total number of line segments
		// That is, all line segments must be EITHER terminal OR external
		return (terminalMap.size() + exteriorLines.size()) == segments.size();
	}
	
	public AbstractBox2f asBox() { return box; }
	
	protected static CompoundBox2f lineToBox(List<LineSegment2f> segments, float width)
	{
		CompoundBox2f box = new CompoundBox2f();
		for(LineSegment2f l : segments)
			box.add(RotaryBox2f.fromLine(l, width));
		
		return box;
	}
	
	/** Returns true if the given point is either end of this passage */
	public boolean isTerminus(Vec2f point)
	{
		Vector2i vec = new Vector2i((int)point.x, (int)point.y);
		return parent.position().equals(vec) || children.stream().map(BlueprintRoom::position).anyMatch(vec::equals);
	}
	
	/** Returns true if the given room is any intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		return room.equals(parent) || children.stream().anyMatch(room::equals);
	}
	
	/** Returns true if this passage shares a parent with the other passage and all end points share the same depth */
	public boolean canShareSpaceWith(BlueprintPassage other)
	{
		final int endDepth = this.children.get(0).metadata().depth();
		final Predicate<RoomMetadata> depthMatch = m -> m.depth() == endDepth;
		return 
				other.parent.equals(this.parent) && 
				other.children.stream().map(BlueprintRoom::metadata).allMatch(depthMatch);
	}
	
	/** Returns true if any line segment in this passage intersects any line segment in the other passage */
	public boolean intersects(BlueprintPassage other)
	{
		return 
				asLines().stream().anyMatch(l -> 
					other.asLines().stream()
					.anyMatch(l2 -> LineSegment2f.doSegmentsIntersect(l, l2)));
	}
	
	/** Returns true if this passage can merge with the other passage */
	public boolean canMergeWith(BlueprintPassage other)
	{
		return 
				canShareSpaceWith(other) &&
				asLines().stream().anyMatch(l -> 
					other.asLines().stream()
					.anyMatch(l2 -> LineSegment2f.doSegmentsIntersect(l, l2)));
	}
	
	public BlueprintPassage mergeWith(BlueprintPassage b)
	{
		int previous = children.size();
		for(BlueprintRoom child : b.children)
			if(children.stream().noneMatch(child::equals))
				children.add(child);
		
		if(children.size() != previous)
		{
			cacheLines();
			box = lineToBox(asLines(), width);
		}
		
		return this;
	}
	
	/** Returns true if this passage intersects with any other unrelated passages in the given chart */
	public boolean intersectsOtherPassages(List<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = BlueprintOrganiser.getPassages(chart);
		return paths.stream()
				// Allow intersection if both passages start from the same parent
				// This promotes the generation of junctions and reduces overall doorway counts
				.filter(Predicates.not(this::canShareSpaceWith))
				.anyMatch(this::intersects);
	}
	
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean intersectsOtherRooms(List<BlueprintRoom> chart)
	{
		List<BlueprintRoom> unrelatedRooms = chart.stream()
				.filter(Predicates.not(parent::equals))
				.filter(Predicates.not(children::contains)).toList();
		
		return unrelatedRooms.stream()
				.map(BlueprintRoom::bounds)
				.map(b -> b.grow(1F))
				.anyMatch(this.box::intersects);
	}
}
