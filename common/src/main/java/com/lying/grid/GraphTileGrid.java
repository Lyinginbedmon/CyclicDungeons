package com.lying.grid;

import java.util.List;
import java.util.Optional;

import net.minecraft.util.math.Direction;

public class GraphTileGrid extends AbstractTileGrid<GridTile>
{
	private Optional<GridTile> min = Optional.empty(), max = Optional.empty();
	
	public AbstractTileGrid<GridTile> addToVolume(GridTile pos)
	{
		final int x=pos.x, y=pos.y;
		if(min.isEmpty())
			min = Optional.of(pos);
		else
		{
			GridTile m = this.min.get();
			if(m.x > x)
				min = Optional.of(new GridTile(x, m.y));
			if(m.y > y)
				min = Optional.of(new GridTile(m.x, y));
		}
		
		if(max.isEmpty())
			max = Optional.of(pos);
		else
		{
			GridTile m = this.max.get();
			if(m.x < x)
				max = Optional.of(new GridTile(x, m.y));
			if(m.y < y)
				max = Optional.of(new GridTile(m.x, y));
		}
		return super.addToVolume(pos);
	}
	
	public boolean contains(GridTile pos)
	{
		final int x=pos.x, y=pos.y;
		if(min.isPresent() && x < min.get().x || y < min.get().y)
			return false;
		if(max.isPresent() && x > max.get().x || y > max.get().y)
			return false;
		return super.contains(pos);
	}
	
	public boolean containsAdjacent(GridTile pos)
	{
		// Any position more than one step outside of the bounds cannot have an adjacency within it
		final int x=pos.x, y=pos.y;
		if(min.isPresent() && x < (min.get().x - 1) || y < (min.get().y - 1))
			return false;
		if(max.isPresent() && x > (max.get().x + 1) || y > (max.get().y + 1))
			return false;
		
		for(Direction face : Direction.Type.HORIZONTAL)
			if(contains(pos.offset(face)))
				return true;
		
		return false;
	}
	
	public List<GridTile> getBoundaries(List<Direction> faces)
	{
		return set.keySet().stream().filter(p -> faces.stream().anyMatch(f -> isBoundary(p, f))).toList();
	}
	
	public boolean isBoundary(GridTile pos, Direction side) { return !contains(pos.offset(side)); }
}
