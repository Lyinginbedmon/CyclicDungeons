package com.lying.blueprint;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.BlueprintTileGrid.TileInstance;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridPathing;
import com.lying.grid.GridPathing.BoundTilePair;
import com.lying.grid.GridTile;
import com.lying.grid.PathingResult;
import com.lying.init.CDTiles;
import com.lying.utility.geometry.AbstractBox2f;
import com.lying.utility.geometry.Box2f;
import com.lying.utility.geometry.LineSegment2f;
import com.lying.utility.logging.DataLog;
import com.lying.worldgen.TileGenerator;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.RotationSupplier;
import com.lying.worldgen.tile.Tile;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class BlueprintPassage
{
	public static final Logger LOGGER	= CyclicDungeons.LOGGER;
	
	/** How many tiles high each passage is */
	public static final int PASSAGE_HEIGHT = 4;
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	public static final int PASSAGE_WIDTH = 3 * TILE_SIZE;
	
	private final BlueprintRoom parent;
	private final List<BlueprintRoom> children = Lists.newArrayList();
	private Box2f box;
	
	private List<GridTile> tilesCached = Lists.newArrayList();
	private Optional<GridTile> startTile = Optional.empty();
	
	private boolean hasFailures = false;
	private DataLog errorLog = new DataLog();
	
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
	}
	
	public BlueprintRoom parent() { return parent; }
	
	public List<BlueprintRoom> children() { return children; }
	
	public int size() { return tiles().size(); }
	
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
		startTile = Optional.empty();
		errorLog.clear();
		errorLog.info("Calculating passageway from {} with {} children", parent, children.size());
		
		// Step 1: Identify median exterior tile of parent room
		final GridTile originExit = findExitDoorway(parent, children, parent.getEntryTile());
		errorLog.info(" # Exit tile: {}", originExit.shortString());
		box = new Box2f(originExit.x, originExit.x, originExit.y, originExit.y);
		
		final List<BlueprintRoom> roomsInvolved = Lists.newArrayList(parent);
		roomsInvolved.addAll(children);
		final Predicate<GridTile> validityCheck = parent.getExclusionCheck();
		
		// Step 2: Build network of tiles from starting doorway to all child rooms
		PathingResult smallestNetwork = PathingResult.failure("Total calculation failure");
		int minLength = Integer.MAX_VALUE;
		PathingResult passage;
		for(BlueprintRoom child : children)
			if((passage = calculateFrom(originExit, child, children, validityCheck)).isSuccess() && passage.result().size() < minLength)
				minLength = (smallestNetwork = passage).size();
			else
				errorLog.warn(passage.failureReason());
		
		// Step 3: Cache resulting network with fewest tiles (ie. the shortest passage)
		if(hasFailures = smallestNetwork.isFailure())
		{
			errorLog.error(smallestNetwork.failureReason());
			return;
		}
		else
		{
			startTile = Optional.of(originExit);
			cacheAll(smallestNetwork.result());
			
			// Step 4: Notify children of selected entryway tile
			for(BlueprintRoom child : children)
			{
				for(GridTile tile : tilesCached)
					if(child.isAdjacent(tile))
					{
						child.setEntryTile(tile);
						break;
					}
			}
		}
	}
	
	/** Returns true if one of more component passages in this passage failed to calculate */
	public boolean containsFailures() { return hasFailures; }
	
	public void reportFailure(Logger logger)
	{
		logger.info("Reporting passage calculation failure");
		this.errorLog.report(logger);
	}
	
	/** Returns the doorway tile of the parent room that this passage originates from */
	@Nullable
	public GridTile getInitialTile() { return startTile.orElse(null); }
	
	/** Returns the doorway tile in the parent room's tile grid that is closest to all child rooms */
	protected static GridTile findExitDoorway(BlueprintRoom parent, List<BlueprintRoom> children, @Nullable GridTile grandParentEntry)
	{
		List<GridTile> parentDoorways = Lists.newArrayList(parent.getDoorwayTiles());
		
		// Exclude the grandparent entryway tile and any tiles immediately adjacent to it
		if(grandParentEntry != null)
			parentDoorways.removeIf(t -> t.isAdjacentOrSame(grandParentEntry));
		
		return GridTile.findClosestToAll(parentDoorways, children.stream().map(BlueprintRoom::tilePosition).toList());
	}
	
	/**
	 * Calculates the network of tiles using the given room as the first to be calculated
	 * @param start	The exit doorway tile of the parent room
	 * @param initial	The first child room to be calculated from
	 * @param successive	All child rooms of the parent room
	 * @param walkable	The predicate defining passable tiles in the tile grid
	 * @return	A PathingResult containing the network of tiles linking all child rooms, if any
	 */
	protected PathingResult calculateFrom(GridTile start, BlueprintRoom initial, List<BlueprintRoom> successive, Predicate<GridTile> walkable)
	{
		/** Set of all tiles within this passageway */
		List<GridTile> tiles = Lists.newArrayList(start);
		
		/** Set of all rooms accessed (not exited) from this passage, with the initial always at index 0 */
		List<BlueprintRoom> rooms = Lists.newArrayList(initial);
		for(BlueprintRoom room : successive)
			if(!rooms.contains(room))
				rooms.add(room);
		
		for(BlueprintRoom child : rooms)
		{
			// Ignore any door tiles that would conflict with a sibling
			final Predicate<GridTile> exclusionCheck = t -> 
					successive.stream()
					.filter(Predicates.not(child::equals))
					.noneMatch(r -> r.occupiesOrIsAdjacent(t));
			
			List<GridTile> doors = child.getDoorwayTiles().stream().filter(exclusionCheck).toList();
			if(doors.isEmpty())
				return PathingResult.failure("No viable doorways found");
			
			errorLog.info(" * {} doors available to join to {}", doors.size(), child);
			BoundTilePair boundPair = GridPathing.findBestCandidatesToJoin(tiles, doors, walkable);
			if(boundPair == null)
				return PathingResult.failure("No viable pairings");
			if(!boundPair.walkable() || boundPair.route().isFailure())
				return boundPair.route();
			
			tiles.addAll(boundPair.route().result());
			tiles.add(boundPair.getRight());
		}
		return PathingResult.success(tiles);
	}
	
	protected void cacheAll(List<GridTile> tiles)
	{
		tiles.forEach(this::cacheTile);
	}
	
	protected void cacheTile(GridTile tile)
	{
		if(tilesCached.stream().noneMatch(tile::equals))
		{
			tilesCached.add(tile);
			
			final int x = tile.x, y = tile.y;
			if(x < box.minX())
				box = new Box2f(x, box.maxX(), box.minY(), box.maxY());
			if(x > box.maxX())
				box = new Box2f(box.minX(), x, box.minY(), box.maxY());
			if(y < box.minY())
				box = new Box2f(box.minX(), box.maxX(), y, box.maxY());
			if(y > box.maxY())
				box = new Box2f(box.minX(), box.maxX(), box.minY(), y);
		}
	}
	
	public GraphTileGrid asTiles()
	{
		return (GraphTileGrid)new GraphTileGrid().addAllToVolume(tiles());
	}
	
	public AbstractBox2f tileBounds()
	{
		tiles();
		return box;
	}
	
	public List<Box> worldBox()
	{
		List<Box> boxes = Lists.newArrayList();
		for(GridTile tile : tiles())
			boxes.add(GridTile.BOX.offset(tile.x * TILE_SIZE, 0, tile.y * TILE_SIZE).withMaxY(PASSAGE_HEIGHT * TILE_SIZE));
		return boxes;
	}
	
	/** Returns true if the given room is any intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		return room.equals(parent) || isEndOf(room);
	}
	
	/** Returns true if the given is one of the descendant rooms connected to this passage */
	public boolean isEndOf(BlueprintRoom room)
	{
		return children.stream().anyMatch(room::equals);
	}
	
	/** Returns true if this passage shares a parent with the other passage and all end points share the same depth */
	public boolean canShareSpaceWith(BlueprintPassage other)
	{
		if(!other.parent.equals(this.parent))
			return false;
		
		final int endDepth = parent.metadata().depth() + 1;
		for(BlueprintRoom otherChild : other.children)
			if(otherChild.metadata().depth() != endDepth)
				return false;
		
		return true;
	}
	
	/** Returns true if any tile in this passage intersects or is adjacent to any tile in the other passage */
	public boolean intersects(BlueprintPassage other)
	{
		final List<GridTile> otherTiles = other.tiles();
		return 
				tiles().stream().anyMatch(l -> 
					otherTiles.stream()
						.anyMatch(l::isAdjacentOrSame));
	}
	
	/** Returns true if any of my tiles are adjacent to any of their tiles */
	public boolean canMergeWith(BlueprintPassage other)
	{
		if(!canShareSpaceWith(other))
			return false;
		
		List<GridTile> myTiles = tiles();
		return other.tiles().stream()
				.anyMatch(p2 -> myTiles.stream()
					.anyMatch(p2::isAdjacentOrSame));
	}
	
	public BlueprintPassage mergeWith(BlueprintPassage b)
	{
		int previous = children.size();
		for(BlueprintRoom child : b.children)
			if(children.stream().noneMatch(child::equals))
				children.add(child);
		
		if(children.size() != previous)
			cacheTiles();
		
		return this;
	}
	
	/** Returns true if this passage intersects with any other unrelated passages in the given chart */
	public boolean intersectsOtherPassages(Blueprint chart)
	{
		// Allow intersection if both passages start from the same parent
		// This promotes the generation of junctions and reduces overall doorway counts
		for(BlueprintPassage passage : chart.passages())
			if(passage.tileBounds().intersects(box) & !canShareSpaceWith(passage) && intersects(passage))
				return true;
		return false;
	}
	
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean intersectsOtherRooms(List<BlueprintRoom> chart)
	{
		final List<GridTile> myTiles = tiles();
		for(BlueprintRoom room : chart)
			if(parent.equals(room) || children.stream().noneMatch(room::equals) || !room.tileBounds().intersects(box))
				continue;
			else if(room.tiles().stream()
					.anyMatch(t -> myTiles.stream()
						.anyMatch(t::isAdjacentOrSame)))
					return true;
		return false;
	}
	
	public void generate(BlockPos origin, ServerWorld world, Random rand)
	{
		BlueprintTileGrid map = BlueprintTileGrid.fromGraphGrid(asTiles(), PASSAGE_HEIGHT);
		
		// Pre-seed doorway from parent room before generating
		final GraphTileGrid parent = parent().tileGrid();
		GridTile doorGrid = getInitialTile();
		if(doorGrid == null)
			return;
		
		BlockPos doorPos = new BlockPos(doorGrid.x, 1, doorGrid.y);
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
		
		final Theme theme = parent().metadata().theme();
		TileGenerator.generate(map, theme.passageTileSet(), rand);
		map.finalise(theme, rand);
		
		// Ensure doorway from parent room has correct orientation
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
