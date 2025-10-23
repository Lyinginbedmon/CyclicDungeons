package com.lying.blueprint;

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
import com.lying.utility.GridTile;
import com.lying.utility.LineSegment2f;
import com.lying.utility.LineUtils;
import com.lying.utility.RotaryBox2f;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class BlueprintPassage
{
	public static final Map<Tile,Float> PASSAGE_TILE_SET = Map.of(
			CDTiles.FLOOR.get(), 10000F,
			CDTiles.AIR.get(), 10F
			);
	
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	public static final int PASSAGE_WIDTH = 3 * TILE_SIZE;
	private final BlueprintRoom parent;
	private final List<BlueprintRoom> children = Lists.newArrayList();
	private final float width;
	private CompoundBox2f box;
	
	private List<LineSegment2f> linesCached = Lists.newArrayList();
	private List<GridTile> tilesCached = Lists.newArrayList();
	
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
	
	public double lengthManhattan()
	{
		double dX = 0D, dY = 0D;
		for(LineSegment2f line : asLines())
		{
			Vec2f delta = line.direction();
			dX += Math.abs(delta.x);
			dY += Math.abs(delta.y);
		}
		return dX + dY;
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
		
		if(children.size() <= 1)
		{
			linesCached.addAll(LineUtils.trialLines(parent, children.getFirst(), this::isLineViable));
			return;
		}
		
		// FIXME Refine merge behaviour
		List<GridTile> positions = Lists.newArrayList();
		positions.add(parent.tilePosition());
		children.stream().map(BlueprintRoom::tilePosition).forEach(positions::add);
		GridTile median = GridTile.median(positions.toArray(new GridTile[0]));
		
		BlueprintRoom junction = BlueprintRoom.create();
		junction.setTilePosition(median);
		junction.metadata().setTileSize(1, 1);
		
		linesCached.addAll(LineUtils.trialLines(parent, junction, this::isLineViable));
		
		children.stream()
			.map(child -> LineUtils.trialLines(junction, child, this::isLineViable))
			.forEach(linesCached::addAll);
	}
	
	public List<GridTile> asTiles()
	{
		if(tilesCached.isEmpty())
			cacheTiles();
		
		return tilesCached;
	}
	
	public void cacheTiles()
	{
		tilesCached.clear();
		tilesCached.addAll(toTiles(asLines()));
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
	
	public Vec2f getStart()
	{
		return asLines().getFirst().getLeft();
	}
	
	public Vec2f getEnd()
	{
		return asLines().getLast().getRight();
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
		
		List<GridTile> tilesInSegment = toTiles(segments);
		List<GridTile> tilesInRooms = Lists.newArrayList();
		rooms.stream().map(BlueprintRoom::tiles).forEach(tilesInRooms::addAll);
		
		int adjacentTally = 0;
		for(GridTile tile : tilesInRooms)
			adjacentTally += tilesInSegment.stream().filter(tile::isAdjacentTo).count();
		
		return adjacentTally <= rooms.size();
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
		final int endDepth = parent.metadata().depth() + 1;
		final Predicate<RoomMetadata> depthMatch = m -> m.depth() == endDepth;
		return 
				other.parent.equals(this.parent) && 
				other.children.stream().map(BlueprintRoom::metadata).allMatch(depthMatch);
	}
	
	/** Returns true if any line segment in this passage intersects any line segment in the other passage */
	public boolean intersects(BlueprintPassage other)
	{
		List<GridTile> otherTiles = other.asTiles();
		return 
				asTiles().stream().anyMatch(l -> 
					otherTiles.stream()
						.anyMatch(l::isAdjacentTo));
	}
	
	/** Returns true if this passage can merge with the other passage */
	public boolean canMergeWith(BlueprintPassage other)
	{
		if(!canShareSpaceWith(other))
			return false;
		
		List<GridTile> myTiles = asTiles();
		return other.asTiles().stream().anyMatch(p2 -> myTiles.stream().anyMatch(p2::isAdjacentTo));
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
			cacheTiles();
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
		List<GridTile> myTiles = asTiles();
		return chart.stream()
				.filter(Predicates.not(parent::equals))
				.filter(Predicates.not(children::contains))
				.map(BlueprintRoom::tiles)
				.anyMatch(l -> l.stream().anyMatch(p1 -> myTiles.stream().anyMatch(p1::isAdjacentTo)));
	}
	
	public static List<GridTile> toTiles(List<LineSegment2f> lines)
	{
		List<GridTile> tiles = Lists.newArrayList();
		for(LineSegment2f line : lines)
			lineToTiles(line).stream().filter(Predicates.not(tiles::contains)).forEach(tiles::add);
		
		return tiles;
	}
	
	public static List<GridTile> lineToTiles(LineSegment2f line)
	{
		List<GridTile> set = Lists.newArrayList(); 
		GridTile startTile = new GridTile(Math.floorDiv((int)line.getLeft().x, TILE_SIZE), Math.floorDiv((int)line.getLeft().y, TILE_SIZE));
		GridTile endTile = new GridTile(Math.floorDiv((int)line.getRight().x, TILE_SIZE), Math.floorDiv((int)line.getRight().y, TILE_SIZE));
		
		// Initial tile population
		int len = (int)(line.length() / TILE_SIZE);
		Vec2f dir = line.direction().normalize();
		for(int i=0; i<len; i++)
		{
			GridTile offset = GridTile.fromVec(dir.multiply(i));
			GridTile tile = startTile.add(offset);
			if(set.stream().noneMatch(tile::equals))
				set.add(tile);
		}
		
		set.add(endTile);
		
		// Walk through population to find each successive tile
		List<GridTile> additions = Lists.newArrayList();
		for(int i=1; i<set.size(); i++)
		{
			GridTile start = set.get(i-1);
			GridTile end = set.get(i);
			
			List<Direction> moves = Direction.Type.HORIZONTAL.stream()
					.filter(d -> d.getOffsetX() == Math.signum(dir.x) || d.getOffsetZ() == Math.signum(dir.y))
					.toList();
			
			while(start.distance(end) > 0)
			{
				Direction bestMove = null;
				double minDist = Double.MAX_VALUE;
				for(Direction move : moves)
				{
					GridTile tile = start.offset(move);
					double dist = tile.distance(end);
					if(dist < minDist)
					{
						bestMove = move;
						minDist = dist;
					}
				}
				
				start = start.offset(bestMove);
				if(additions.stream().noneMatch(start::equals))
					additions.add(start);
			}
		}
		
		additions.removeIf(set::contains);
		set.addAll(additions);
		return set;
	}
}
