package com.lying.utility;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

/** A grid-aligned 2D bounding box */
public class Box2f extends AbstractBox2f
{
	private final float minX, minY, maxX, maxY;
	private final Map<Direction, LineSegment2f> edgeMap;
	
	public Box2f(float minX, float maxX, float minY, float maxY)
	{
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		
		Vec2f 
			a = new Vec2f(minX, minY), 
			b = new Vec2f(maxX, minY), 
			c = new Vec2f(maxX, maxY), 
			d = new Vec2f(minX, maxY);
		
		edgeMap = Map.of(
			Direction.NORTH, new LineSegment2f(a, b),
			Direction.EAST, new LineSegment2f(b, c),
			Direction.SOUTH, new LineSegment2f(c, d),
			Direction.WEST, new LineSegment2f(d, a)
				);
	}
	
	public String toString() { return "Box["+minX+"->"+maxX+", "+minY+"->"+maxY+"]"; }
	
	public final float minX() { return minX; }
	public final float minY() { return minY; }
	public final float maxX() { return maxX; }
	public final float maxY() { return maxY; }
	
	public List<LineSegment2f> asEdges()
	{
		return Lists.newArrayList(edgeMap.values());
	}
	
	public List<Vec2f> asPoints()
	{
		return List.of(
				new Vec2f(minX, minY),
				new Vec2f(maxX, minY),
				new Vec2f(maxX, maxY),
				new Vec2f(minX, maxY)
				);
	}
	
	public boolean intersects(AbstractBox2f box)
	{
		if(box.asPoints().stream().anyMatch(p -> 
		{
			return 
					p.x >= minX && p.x <= maxX &&
					p.y >= minY && p.y <= maxY;  
		}))
			return true;
		
		return super.intersects(box);
	}
	
	public boolean contains(Vec2f vec)
	{
		return 
				vec.x >= minX && vec.x <= maxX && 
				vec.y >= minY && vec.y <= maxY;
	}
	
	public AbstractBox2f move(Vec2f vec)
	{
		return new Box2f(minX + vec.x, maxX + vec.x, minY + vec.y, maxY + vec.y);
	}
	
	public AbstractBox2f mul(float scalar)
	{
		return new Box2f(minX * scalar, maxX * scalar, minY * scalar, maxY * scalar);
	}
	
	public AbstractBox2f grow(float v)
	{
		return new Box2f(minX - v, maxX + v, minY - v, maxY + v);
	}
	
	@Nullable
	public LineSegment2f getEdge(Direction dir)
	{
		return edgeMap.getOrDefault(dir, null);
	}
	
	@Nullable
	public Direction identifyEdge(LineSegment2f faceIn)
	{
		for(Direction edge : Direction.Type.HORIZONTAL)
			if(faceIn.equals(getEdge(edge)))
				return edge;
		return null;
	}
}