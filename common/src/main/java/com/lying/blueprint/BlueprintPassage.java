package com.lying.blueprint;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.BlueprintTileGrid.TileInstance;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridPathing;
import com.lying.grid.GridPathing.BoundTilePair;
import com.lying.grid.GridTile;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.utility.LineUtils;
import com.lying.worldgen.TileGenerator;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.RotationSupplier;
import com.lying.worldgen.tile.Tile;
import com.mojang.datafixers.util.Pair;

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
		/**
		 * * Identify closest exterior tile of parent to all children
		 * * Build lines to child rooms from that median tile
		 * * Bridge from extant tiles where possible
		 * * Select resulting network with fewest tiles
		 */
		tilesCached.clear();
		
		// Step 1: Identify median exterior tile of parent room
		final GridTile originExit = findExitDoorway(parent, children, parent.getEntryTile());
		
		final List<BlueprintRoom> roomsInvolved = Lists.newArrayList(parent);
		roomsInvolved.addAll(children);
		final Predicate<GridTile> validityCheck = t -> roomsInvolved.stream().noneMatch(r -> r.occupiesOrIsAdjacent(t));
		
		// Step 2: Build network of tiles from starting doorway to all child rooms
		List<GridTile> shortestPassage = null;
		int minLength = Integer.MAX_VALUE;
		for(BlueprintRoom child : children)
		{
			List<GridTile> passage = calculateFrom(originExit, child, children, validityCheck);
			if(passage.size() < minLength)
			{
				minLength = passage.size();
				shortestPassage = passage;
			}
		}
		
		// Step 3: Cache resulting network with fewest tiles (ie. the shortest passage)
		cacheAll(shortestPassage);
		
		// Step 4: Notify children of selected entryway tile
		for(BlueprintRoom child : children)
			child.tileGrid()
				.getDoorwayTiles().stream()
				.filter(shortestPassage::contains)
				.findFirst()
				.ifPresent(child::setEntryTile);
	}
	
	/** Returns the doorway tile in the parent room's tile grid that is closest to all child rooms */
	protected static GridTile findExitDoorway(BlueprintRoom parent, List<BlueprintRoom> children, @Nullable GridTile grandParentEntry)
	{
		List<GridTile> childPositions = children.stream().map(BlueprintRoom::tilePosition).toList();
		final GraphTileGrid parentGrid = parent.tileGrid();
		List<GridTile> parentDoorways = parentGrid.getDoorwayTiles();
		
		// Exclude the grandparent entryway tile and any tiles immediately adjacent to it
		if(grandParentEntry != null)
			parentDoorways = parentDoorways.stream().filter(t -> t.manhattanDistance(grandParentEntry) > 1).toList();
		
		return GridTile.findClosestToAll(parentDoorways, childPositions);
	}
	
	/** Calculates the network of tiles using the given room as the first to be calculated */
	protected static List<GridTile> calculateFrom(GridTile start, BlueprintRoom initial, List<BlueprintRoom> successive, Predicate<GridTile> validityCheck)
	{
		List<GridTile> tiles = Lists.newArrayList(start);
		
		List<BlueprintRoom> rooms = Lists.newArrayList(initial);
		successive.stream().filter(r -> !r.uuid().equals(initial.uuid())).forEach(rooms::add);
		
		for(BlueprintRoom child : rooms)
		{
			// Ignore any door tiles that would be too close to a sibling room
			final List<BlueprintRoom> exclusion = successive.stream().filter(r -> !r.uuid().equals(child.uuid())).toList();
			final Predicate<GridTile> exclusionCheck = t -> exclusion.stream().noneMatch(r -> r.occupiesOrIsAdjacent(t));
			
			BoundTilePair fromPassageToDoor = GridPathing.findBestCandidatesToJoin(child.tileGrid().getDoorwayTiles().stream().filter(exclusionCheck).toList(), tiles, validityCheck);
			if(fromPassageToDoor == null)
				continue;
			
			tiles.addAll(fromPassageToDoor.route());
			tiles.add(fromPassageToDoor.getRight());
		}
		return tiles;
	}
	
	protected void cacheTile(GridTile tile)
	{
		if(tilesCached.stream().noneMatch(tile::equals))
			tilesCached.add(tile);
	}
	
	protected void cacheAll(List<GridTile> tiles)
	{
		tiles.forEach(this::cacheTile);
	}
	
	/** Finds the door tile outside of the given room closest to the target, returns that tile and its relative direction from the room */
	protected static Pair<GridTile,Direction> findDoorPosition(BlueprintRoom room, GridTile target)
	{
		final GraphTileGrid childGrid = room.tileGrid();
		List<GridTile> childBoundaries = childGrid.getDoorwayTiles();
		GridTile childBoundary = GridTile.findClosestTo(childBoundaries, target);
		Direction childStep = Direction.Type.HORIZONTAL.stream().filter(d -> !childGrid.contains(childBoundary.offset(d))).findFirst().get();
		return Pair.of(childBoundary.offset(childStep), childStep);
	}
	
	public GraphTileGrid asTiles()
	{
		return (GraphTileGrid)new GraphTileGrid().addAllToVolume(tiles());
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
						.anyMatch(l::isAdjacentOrSame));
	}
	
	/** Returns true if this passage can merge with the other passage */
	public boolean canMergeWith(BlueprintPassage other)
	{
		if(!canShareSpaceWith(other))
			return false;
		
		List<GridTile> myTiles = tiles();
		// Return true if any of my tiles are adjacent to any of their tiles
		if(other.tiles().stream().anyMatch(p2 -> myTiles.stream().anyMatch(p2::isAdjacentOrSame)))
			return true;
		
		// Return true if any tiles immediately adjacent to my tiles are also adajcent to any of their tiles
//		List<GridTile> adjacents = Lists.newArrayList();
//		myTiles.forEach(t -> Direction.Type.HORIZONTAL.stream().map(t::offset).filter(Predicates.not(adjacents::contains)).forEach(adjacents::add));
//		return other.tiles().stream().anyMatch(p2 -> adjacents.stream().anyMatch(p2::isAdjacentTo));
		
		return false;
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
								.anyMatch(t::isAdjacentOrSame));
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
			map.put(doorPos.down(), CDTiles.instance().getElse(DefaultTiles.ID_PRISTINE_FLOOR, CDTiles.STONE));
			map.put(doorPos, CDTiles.instance().getElse(CDTiles.ID_DOORWAY_LINTEL, CDTiles.STONE));
			if(PASSAGE_HEIGHT > 2)
			{
				BlockPos pos = doorPos.up();
				
				// Place a lintel above the door
				map.put(pos, CDTiles.instance().getElse(CDTiles.ID_DOORWAY_LINTEL, CDTiles.STONE));
				
				// Fill remaining vertical space above the door with boundary
				while(map.contains(pos.up()))
					map.put((pos = pos.up()), CDTiles.instance().getElse(DefaultTiles.ID_PASSAGE_BOUNDARY, CDTiles.AIR));
			}
			break;
		}
		
		final Theme theme = parent().metadata().theme();
		TileGenerator.generate(map, theme.passageTileSet(), world.random);
		map.finalise(theme);
		
		// Ensure doorway from parent room has correct orientation
		if(doorPos != null)
			for(Direction face : Direction.Type.HORIZONTAL)
				if(parent.contains(doorGrid.offset(face)))
				{
					BlockRotation rotation = RotationSupplier.faceToRotationMap.get(face);
					map.finalise(new TileInstance(doorPos, CDTiles.instance().getElse(CDTiles.ID_DOORWAY, CDTiles.AIR), theme, rotation));
					
					if(map.contains(doorPos.up()))
						map.finalise(new TileInstance(doorPos.up(), CDTiles.instance().getElse(CDTiles.ID_DOORWAY_LINTEL, CDTiles.STONE), theme, rotation));
					break;
				}
		
		map.generate(origin, world);
	}
}
