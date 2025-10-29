package com.lying.grid;

import java.util.List;

import org.joml.Math;

import net.minecraft.util.math.Direction;

public class GraphTileGrid extends AbstractTileGrid<GridTile>
{
	public AbstractTileGrid<GridTile> addToVolume(GridTile from, GridTile to)
	{
		int minX = Math.min(from.x, to.x);
		int maxX = Math.max(from.x, to.x);
		
		int minY = Math.min(from.y, to.y);
		int maxY = Math.max(from.y, to.y);
		
		for(int x = minX; x < maxX; x++)
			for(int y = minY; y < maxY; y++)
				addToVolume(new GridTile(x, y));
		return this;
	}
	
	public void grow(Direction direction, int size)
	{
		getBoundaries(List.of(direction)).forEach(p -> 
		{
			for(int i=0; i<size; i++)
				addToVolume(p.offset(direction));
		});
	}
	
	public List<GridTile> getBoundaries(List<Direction> faces)
	{
		return set.keySet().stream().filter(p -> faces.stream().anyMatch(f -> isBoundary(p, f))).toList();
	}
	
	public boolean isBoundary(GridTile pos) { return Direction.Type.HORIZONTAL.stream().anyMatch(d -> isBoundary(pos, d)); }
	
	public boolean isBoundary(GridTile pos, Direction side) { return !contains(pos.offset(side)); }
}
