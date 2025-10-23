package com.lying.utility;

import org.joml.Math;
import org.joml.Vector2i;

import com.lying.worldgen.Tile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class GridTile
{
	public final int x, y;
	
	public GridTile(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public String toString() { return "GridTile["+x+", "+y+"]"; }
	
	public boolean equals(Object obj) { return obj instanceof GridTile && manhattanDistance((GridTile)obj) == 0; }
	
	public GridTile copy() { return new GridTile(x, y); }
	
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
	
	public static GridTile fromVec(Vec2f vec)
	{
		return new GridTile(
				(int)(vec.x - vec.x%Tile.TILE_SIZE), 
				(int)(vec.y - vec.y%Tile.TILE_SIZE)
				);
	}
	
	public Vec2f toVec2f() { return new Vec2f(x, y).add(0.5F); }
	
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
	
	public int manhattanDistance(GridTile tile)
	{
		return Math.abs(x - tile.x) + Math.abs(y - tile.y);
	}
	
	/** If the manhattan distance between any two tiles is less than or equal to 1, they must be overlapping or immediately adjacent */
	public boolean isAdjacentTo(GridTile tile)
	{
		return manhattanDistance(tile) <= 1;
	}
	
	public Box2f bounds()
	{
		return new Box2f(x, x + 1, y, y + 1);
	}
	
	public GridTile offset(Direction dir)
	{
		return offset(dir, 1);
	}
	
	public GridTile offset(Direction dir, int distance)
	{
		return new GridTile(x + dir.getOffsetX() * distance, y + dir.getOffsetZ() * distance);
	}
	
	public GridTile add(Vector2i vec) { return add(vec.x, vec.y); }
	
	public GridTile add(int xIn, int yIn) { return new GridTile(x + xIn, y + yIn); }
	
	public GridTile add(GridTile tile) { return add(tile.x, tile.y); }
	
	public GridTile sub(Vector2i vec) { return sub(vec.x, vec.y); }
	
	public GridTile sub(int xIn, int yIn) { return new GridTile(x - xIn, y - yIn); }
	
	public BlockPos toPos(int yIn) { return new BlockPos(x, yIn, y); }
	
	public GridTile mul(int scalar) { return new GridTile(x * scalar, y * scalar); }
}
