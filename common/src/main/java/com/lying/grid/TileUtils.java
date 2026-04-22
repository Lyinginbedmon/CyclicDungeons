package com.lying.grid;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.utility.Line2f;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class TileUtils
{
	private static final int TILE_SIZE = Tile.TILE_SIZE;
	
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
