package com.lying.grid;

import java.util.List;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintRoom;
import com.lying.utility.Line2f;
import com.lying.utility.LineSegment2f;
import com.lying.utility.LineUtils;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class TileUtils
{
	private static final int TILE_SIZE = Tile.TILE_SIZE;
	
	/** Attempts to generate a viable deterministic route between grid tiles */
	public static List<GridTile> trialTiles(BlueprintRoom start, BlueprintRoom end)
	{
		List<GridTile> tiles = Lists.newArrayList();
		tiles.addAll(toTiles(LineUtils.trialLines(start, end)));
		// TODO Ensure tiles of passage don't include those occupied by either room
		tiles.removeIf(t -> start.tileGrid().contains(t) || end.tileGrid().contains(t));
		return tiles;
	}
	
	public static List<GridTile> spanRooms(BlueprintRoom start, BlueprintRoom end)
	{
		final GridTile startPos = start.tilePosition();
		final GridTile endPos = end.tilePosition();
		
		GraphTileGrid startGrid = start.tileGrid();
		GraphTileGrid endGrid = end.tileGrid();
		
		// Closest non-corner boundary tile in one room to the core of the other
		GridTile startTile = startGrid.getBoundaries().stream()
				.filter(p -> Direction.Type.HORIZONTAL.stream().filter(d -> startGrid.isBoundary(p, d)).count() == 1)
				.sorted(GridTile.distSort(endPos))
				.findFirst().get();
		GridTile endTile = endGrid.getBoundaries().stream()
				.filter(p -> Direction.Type.HORIZONTAL.stream().filter(d -> endGrid.isBoundary(p, d)).count() == 1)
				.sorted(GridTile.distSort(startPos))
				.findFirst().get();
		
		// The direction of the boundary tiles to outside of the corresponding room
		Direction startFace = Direction.Type.HORIZONTAL.stream()
				.filter(d -> startGrid.isBoundary(startTile, d))
				.findFirst().get();
		Direction endFace = Direction.Type.HORIZONTAL.stream()
				.filter(d -> endGrid.isBoundary(endTile, d))
				.findFirst().get();
		
		// The terminator end tiles of the passage
		GridTile startDoor = startTile.offset(startFace);
		GridTile endDoor = endTile.offset(endFace);
		
		// If the doors are equal, just return one of them
		if(startDoor.equals(endDoor))
			return List.of(startDoor);
		
		// If the doors are adjacent, just return the doors
		if(startDoor.isAdjacentTo(endDoor))
			return List.of(startDoor, endDoor);
		
		GridTile outOfStart = startDoor.offset(startFace);
		GridTile outOfEnd = endDoor.offset(endFace);
		
		// If there's only one tile between the doors, return the doors and that interstitial tile
		if(outOfStart.equals(outOfEnd))
			return List.of(startDoor, outOfStart, endDoor);
		
		// Calculate bridging space between the points adjacent to the doorways
		List<GridTile> occupiedFaces = Lists.newArrayList();
		occupiedFaces.add(startDoor);
		occupiedFaces.addAll(adjoinTiles(outOfStart, outOfEnd));
		occupiedFaces.add(endDoor);
		
		return occupiedFaces;
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
		if(line.length() <= TILE_SIZE)
		{
			Vec2f point = line.getLeft().add(line.direction().multiply(0.5F));
			return List.of(new GridTile(Math.floorDiv((int)point.x, TILE_SIZE), Math.floorDiv((int)point.y, TILE_SIZE)));
		}
		
		GridTile startTile = new GridTile(Math.floorDiv((int)line.getLeft().x, TILE_SIZE), Math.floorDiv((int)line.getLeft().y, TILE_SIZE));
		GridTile endTile = new GridTile(Math.floorDiv((int)line.getRight().x, TILE_SIZE), Math.floorDiv((int)line.getRight().y, TILE_SIZE));
		return lineToTiles(line, startTile, endTile);
	}
	
	public static List<GridTile> lineToTiles(LineSegment2f line, GridTile startTile, GridTile endTile)
	{
		List<GridTile> set = Lists.newArrayList(); 
		
		// Initial tile population
		int len = (int)(line.length() / TILE_SIZE);
		Vec2f dir = line.direction().normalize();
		for(int i=0; i<len; i++)
		{
			GridTile offset = GridTile.fromVec(dir.multiply(i));
			GridTile tile = startTile.add(offset);
			if(!set.contains(tile))
				set.add(tile);
		}
		if(!set.contains(endTile))
			set.add(endTile);
		
		// Walk through population to ensure each successive tile is connected
		List<GridTile> additions = Lists.newArrayList();
		for(int i=1; i<set.size(); i++)
			additions.addAll(adjoinTiles(set.get(i-1), set.get(i)));
		
		additions.removeIf(set::contains);
		set.addAll(additions);
		return set;
	}
	
	private static List<GridTile> adjoinTiles(GridTile start, GridTile end)
	{
		List<GridTile> adjoin = Lists.newArrayList();
		
		Line2f line = new Line2f(start.toVec2i(), end.toVec2i());
		int minX = Math.min(start.x, end.x), maxX = Math.max(start.x, end.x);
		int minY = Math.min(start.y, end.y), maxY = Math.max(start.y, end.y);
		// Vertical lines only need a linear scan along the Y axis
		if(line.isVertical)
			for(int y = minY; y <= maxY; y++)
				adjoin.add(new GridTile(start.x, y));
		// Horizontal lines only need a linear scan along the X axis
		else if(line.isHorizontal)
			for(int x = minX; x <= maxX; x++)
				adjoin.add(new GridTile(x, start.y));
		// Angled lines require a proper grid scan
		else
			for(int x = minX; x <= maxX; x++)
				for(int y = minY; y <= maxY; y++)
				{
					GridTile tile = new GridTile(x, y);
					double dist = Math.sqrt(tile.toVec2f().distanceSquared(line.atX(x)));
					if(dist < 0.75F)
						adjoin.add(tile);
				}
		
		adjoin.sort(GridTile.distSort(start));
		List<GridTile> bridging = Lists.newArrayList();
		for(int i=1; i<adjoin.size(); i++)
		{
			GridTile a = adjoin.get(i - 1);
			GridTile b = adjoin.get(i);
			if(a.manhattanDistance(b) > 1)
				bridging.addAll(walkBetween(a,b));
		}
		adjoin.addAll(bridging);
		adjoin.sort(GridTile.distSort(start));
		
		return adjoin;
	}
	
	public static List<GridTile> walkBetween(GridTile start, GridTile end)
	{
		List<GridTile> walk = Lists.newArrayList();
		
		GridTile point = start;
		while(point.distance(end) > 0)
		{
			final GridTile tile = point;
			Direction step = Direction.Type.HORIZONTAL.stream().sorted((a,b) -> 
			{
				double aD = tile.offset(a).distance(end);
				double bD = tile.offset(b).distance(end);
				return aD < bD ? -1 : aD > bD ? 1 : 0;
			}).findFirst().get();
			
			walk.add(point = point.offset(step));
		}
		
		return walk;
	}
}
