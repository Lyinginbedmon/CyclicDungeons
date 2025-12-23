package com.lying.blueprint;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.BlueprintTileGrid.TileInstance;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridTile;
import com.lying.grid.TileUtils;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.utility.LineUtils;
import com.lying.utility.RotaryBox2f;
import com.lying.worldgen.Tile;
import com.lying.worldgen.Tile.RotationSupplier;
import com.lying.worldgen.TileGenerator;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class BlueprintPassage
{
	/** How many tiles high each passage is */
	public static final int PASSAGE_HEIGHT = 4;
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	public static final int PASSAGE_WIDTH = 3 * TILE_SIZE;
	public static final Map<Tile, Float> PASSAGE_TILE_SET = Map.of(
			CDTiles.PASSAGE_FLOOR.get(), 10000F,
			CDTiles.AIR.get(), 10F,
			CDTiles.FLOOR_LIGHT.get(), 1F
			);
	
	private final BlueprintRoom parent;
	private final List<BlueprintRoom> children = Lists.newArrayList();
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
		buildBounds();
	}
	
	public BlueprintRoom parent() { return parent; }
	
	public List<BlueprintRoom> children() { return children; }
	
	public BlueprintPassage addChild(BlueprintRoom room)
	{
		if(children.stream().noneMatch(room::equals))
		{
			children.add(room);
			buildBounds();
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
			linesCached.addAll(LineUtils.trialLines(parent, children.getFirst()));
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
		
		linesCached.addAll(LineUtils.trialLines(parent, junction));
		
		children.stream()
			.map(child -> LineUtils.trialLines(junction, child))
			.forEach(linesCached::addAll);
	}
	
	public List<GridTile> tiles()
	{
		if(tilesCached.isEmpty())
			cacheTiles();
		
		return tilesCached;
	}
	
	public void cacheTiles()
	{
		tilesCached.clear();
		children.forEach(child -> 
			TileUtils.trialTiles(parent, child).stream()
				.filter(Predicates.not(tilesCached::contains))
				.forEach(tilesCached::add));
	}
	
	public GraphTileGrid asTiles()
	{
		return (GraphTileGrid)new GraphTileGrid().addAllToVolume(tiles());
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
	
	public AbstractBox2f tileBounds() { return box; }
	
	public List<Box> worldBox()
	{
		List<Box> boxes = Lists.newArrayList();
		for(GridTile tile : tiles())
			boxes.add(GridTile.BOX.offset(tile.x * TILE_SIZE, 0, tile.y * TILE_SIZE).withMaxY(PASSAGE_HEIGHT * TILE_SIZE));
		return boxes;
	}
	
	protected void buildBounds()
	{
		box = new CompoundBox2f();
		for(GridTile tile : tiles())
			box.add(GridTile.BOUNDS.move(new Vec2f(tile.x, tile.y)));
	}
	
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
		List<GridTile> otherTiles = other.tiles();
		return 
				tiles().stream().anyMatch(l -> 
					otherTiles.stream()
						.anyMatch(l::isAdjacentTo));
	}
	
	/** Returns true if this passage can merge with the other passage */
	public boolean canMergeWith(BlueprintPassage other)
	{
		if(!canShareSpaceWith(other))
			return false;
		
		List<GridTile> myTiles = tiles();
		return other.tiles().stream().anyMatch(p2 -> myTiles.stream().anyMatch(p2::isAdjacentTo));
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
			buildBounds();
		}
		
		return this;
	}
	
	/** Returns true if this passage intersects with any other unrelated passages in the given chart */
	public boolean intersectsOtherPassages(Blueprint chart)
	{
		List<BlueprintPassage> paths = chart.passages();
		return paths.stream()
				// Allow intersection if both passages start from the same parent
				// This promotes the generation of junctions and reduces overall doorway counts
				.filter(Predicates.not(this::canShareSpaceWith))
				.anyMatch(this::intersects);
	}
	
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean intersectsOtherRooms(List<BlueprintRoom> chart)
	{
		List<GridTile> myTiles = tiles();
		return chart.stream()
				.filter(p -> !parent.equals(p) && children.stream().noneMatch(p::equals))
				.anyMatch(r -> 
				{
					// Intersection if we occupy any of the same tiles as the room
					// Intersection if any of our tiles are adjacent to any tile of the room
					// This is avoided to keep passages navigable, given potential intrusion by the exterior shell
					return r.tiles().stream()
							.anyMatch(t -> myTiles.stream()
								.anyMatch(t::isAdjacentTo));
				});
	}
	
	public void generate(BlockPos origin, ServerWorld world)
	{
		BlueprintTileGrid map = BlueprintTileGrid.fromGraphGrid(asTiles(), PASSAGE_HEIGHT);
		
		// Pre-seed doorway from parent room before generating
		final GraphTileGrid parent = parent().tileGrid();
		BlockPos doorPos = null;
		GridTile doorGrid = null;
		for(BlockPos p : map.contents())
		{
			doorGrid = new GridTile(p.getX(), p.getZ());
			if(!parent.containsAdjacent(doorGrid))
				continue;
			
			doorPos = p.withY(1);
			map.put(doorPos.down(), CDTiles.FLOOR_PRISTINE.get());
			map.put(doorPos, CDTiles.DOORWAY.get());
			if(PASSAGE_HEIGHT > 2)
			{
				map.put(doorPos.up(), CDTiles.DOORWAY_LINTEL.get());
				
				BlockPos tile = doorPos.up(2);
				while(map.contains(tile))
				{
					map.put(tile, CDTiles.PASSAGE_BOUNDARY.get());
					tile = tile.up();
				}
			}
			break;
		}
		
		TileGenerator.generate(map, PASSAGE_TILE_SET, world.random);
		map.finalise();
		
		// Ensure doorway from parent room has correct orientation
		if(doorPos != null)
			for(Direction face : Direction.Type.HORIZONTAL)
				if(parent.contains(doorGrid.offset(face)))
				{
					BlockRotation rotation = RotationSupplier.faceToRotationMap.get(face);
					map.finalise(new TileInstance(doorPos, CDTiles.DOORWAY.get(), rotation));
					if(PASSAGE_HEIGHT > 2)
						map.finalise(new TileInstance(doorPos.up(), CDTiles.DOORWAY_LINTEL.get(), rotation));
					break;
				}
		
		map.generate(origin, world);
	}
}
