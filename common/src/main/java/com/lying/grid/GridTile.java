package com.lying.grid;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector2i;

import com.lying.utility.AbstractBox2f;
import com.lying.utility.Box2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class GridTile
{
	public static final int GRID_SIZE	= Tile.TILE_SIZE;
	public static final GridTile ZERO	= new GridTile(0,0);
	public static final AbstractBox2f BOUNDS	= new Box2f(0, GRID_SIZE, 0, GRID_SIZE);
	public static final Box BOX					= new Box(0, 0, 0, GRID_SIZE, GRID_SIZE, GRID_SIZE);
	public final int x, y;
	
	public GridTile(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public String toString() { return "GridTile"+shortString(); }
	
	public String shortString() { return "["+x+", "+y+"]"; }
	
	public boolean equals(Object obj) { return obj instanceof GridTile && manhattanDistance((GridTile)obj) == 0; }
	
	public GridTile copy() { return new GridTile(x, y); }
	
	public static Comparator<GridTile> distSort(GridTile target)
	{
		return (a,b) -> 
		{
			double aD = a.distance(target);
			double bD = b.distance(target);
			return aD < bD ? -1 : aD > bD ? 1 : 0;
		};
	}
	
	public static GridTile median(GridTile... tiles)
	{
		float x = 0;
		float y = 0;
		for(GridTile tile : tiles)
		{
			x += tile.x;
			y += tile.y;
		}
		x /= tiles.length;
		y /= tiles.length;
		
		return fromVec(new Vec2f(x, y));
	}
	
	public static GridTile worldVecToGrid(Vec2f vec)
	{
		return fromVec(vec.multiply(1 / GRID_SIZE));
	}
	
	public static GridTile fromVec(Vec2f vec)
	{
		return new GridTile(
				(int)(vec.x - vec.x%GRID_SIZE), 
				(int)(vec.y - vec.y%GRID_SIZE)
				);
	}
	
	public Vec2f toVec2f() { return new Vec2f(x, y).add(0.5F); }
	
	public Vec2f gridToWorldVec() { return mul(GRID_SIZE).toVec2f(); }
	
	public Vector2i toVec2i() { return new Vector2i(x, y); }
	
	public double distance(GridTile tile)
	{
		return distance(tile.x, tile.y);
	}
	
	public double distance(int xIn, int yIn)
	{
		double x = this.x - xIn;
		double y = this.y - yIn;
		return Math.sqrt(x * x + y * y);
	}
	
	/** Returns the sum of the difference on the X and Y axis from this tile to the given tile */
	public int manhattanDistance(GridTile tile)
	{
		return Math.abs(x - tile.x) + Math.abs(y - tile.y);
	}
	
	/** If the manhattan distance between any two tiles is less than or equal to 1, they must be overlapping or immediately adjacent */
	public boolean isAdjacentOrSame(GridTile tile)
	{
		return manhattanDistance(tile) <= 1;
	}
	
	/** Returns true if the manhattan distance between this tile and the given tile is exactly 1 */
	public boolean isAdjacent(GridTile tile)
	{
		return manhattanDistance(tile) == 1;
	}
	
	/** Returns true if this tile and the given tile are aligned on at least one 2D axis */
	public boolean isParallel(GridTile tile) { return x == tile.x || y == tile.y; }
	
	public Box2f bounds()
	{
		return new Box2f(x, x + 1, y, y + 1);
	}
	
	public GridTile offset(@Nullable Direction dir)
	{
		return offset(dir, 1);
	}
	
	public GridTile offset(@Nullable Direction dir, int distance)
	{
		if(dir == null || distance == 0)
			return copy();
		else
			return new GridTile(x + dir.getOffsetX() * distance, y + dir.getOffsetZ() * distance);
	}
	
	public GridTile add(Vector2i vec) { return add(vec.x, vec.y); }
	
	public GridTile add(int xIn, int yIn) { return new GridTile(x + xIn, y + yIn); }
	
	public GridTile add(GridTile tile) { return add(tile.x, tile.y); }
	
	public GridTile sub(Vector2i vec) { return sub(vec.x, vec.y); }
	
	public GridTile sub(int xIn, int yIn) { return add(-xIn, -yIn); }
	
	public BlockPos toPos(int yIn) { return new BlockPos(x, yIn, y); }
	
	public GridTile mul(int scalar) { return new GridTile(x * scalar, y * scalar); }
	
	@Nullable
	public static GridTile findClosestTo(List<GridTile> tiles, @Nullable GridTile target)
	{
		if(target == null)
			throw new NullPointerException("GridTile.findClosestTo received null target tile");
		else if(tiles == null || tiles.isEmpty())
			throw new UnsupportedOperationException("GridTile.findClosestTo received no tile list");
		
		GridTile closest = null;
		double minDist = Double.MAX_VALUE;
		for(GridTile tile : tiles)
		{
			double dist = tile.distance(target);
			if(dist < minDist)
			{
				minDist = dist;
				closest = tile;
			}
		}
		return closest;
	}
	
	@Nullable
	public static GridTile findClosestToAll(List<GridTile> tiles, List<GridTile> targets)
	{
		GridTile closest = null;
		double minDist = Double.MAX_VALUE;
		for(GridTile tile : tiles)
		{
			double dist = 0D;
			for(GridTile child : targets)
				dist += tile.distance(child);
			dist /= targets.size();
			
			if(dist < minDist)
			{
				minDist = dist;
				closest = tile;
			}
		};
		return closest;
	}
}
