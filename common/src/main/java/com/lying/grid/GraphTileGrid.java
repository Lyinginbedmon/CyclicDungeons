package com.lying.grid;

import java.util.List;

import org.joml.Math;

import com.google.common.collect.Lists;

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
	
	public boolean containsAdjacent(GridTile pos)
	{
		return set.keySet().stream().anyMatch(p2 -> p2.manhattanDistance(pos) == 1);
	}
	
	public boolean containsOrAdjacentTo(GridTile pos)
	{
		return contains(pos) || containsAdjacent(pos);
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
	
	/** Returns a list of all tiles adjacent to this grid that can be used as doorways */
	public List<GridTile> getDoorwayTiles()
	{
		// List of boundary tiles, excluding corners
		List<GridTile> childBoundaries = Lists.newArrayList(getBoundaries());
		childBoundaries.removeIf(t -> Direction.Type.HORIZONTAL.stream().map(t::offset).filter(this::contains).count() < 3);
		
		// Calculate step direction outwards from each tile and offset
		return childBoundaries.stream().map(tile -> 
		{
			// Direction from boundary tile that exits the local grid
			Direction childStep = Direction.Type.HORIZONTAL.stream().filter(d -> !this.contains(tile.offset(d))).findFirst().get();
			return tile.offset(childStep);
		}).toList();
	}
}
