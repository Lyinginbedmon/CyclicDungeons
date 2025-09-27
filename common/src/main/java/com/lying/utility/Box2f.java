package com.lying.utility;

import java.util.List;

import net.minecraft.util.math.Vec2f;

public class Box2f
{
	private float minX, minY, maxX, maxY;
	
	public Box2f(float minX, float maxX, float minY, float maxY)
	{
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	public final float minX() { return minX; }
	public final float minY() { return minY; }
	public final float maxX() { return maxX; }
	public final float maxY() { return maxY; }
	
	public Box2f offset(Vec2f vec)
	{
		return offset(vec.x, vec.y);
	}
	
	public Box2f offset(float x, float y)
	{
		return new Box2f(minX + x, maxX + x, minY + y, maxY + y);
	}
	
	public Box2f scale(float scalar)
	{
		return new Box2f(minX * scalar, maxX * scalar, minY * scalar, maxY * scalar);
	}
	
	public Box2f grow(float x, float y)
	{
		return new Box2f(minX - x, maxX + x, minY - y, maxY + y);
	}
	
	public Box2f grow(float v)
	{
		return grow(v, v);
	}
	
	public boolean intersects(Box2f box)
	{
		return intersects(box.minX, box.minY, box.maxX, box.maxY);
	}
	
	public boolean intersects(float minX, float minY, float maxX, float maxY)
	{
		return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY;
	}
	
	public boolean intersects(Line2f line)
	{
		// If the bounds contain either point of a line, it must intersect
		if(contains(line.getLeft()) || contains(line.getRight()))
			return true;
		
		// If the line intersects any boundary line of the bounds, it must intersect
		return asEdges().stream().anyMatch(line::intersects);
	}
	
	public boolean contains(Vec2f vec)
	{
		return vec.x > minX && vec.x < maxX && vec.y > minY && vec.y < maxY;
	}
	
	public List<Line2f> asEdges()
	{
		Vec2f 
			a = new Vec2f((float)minX, (float)minY), 
			b = new Vec2f((float)maxX, (float)minY), 
			c = new Vec2f((float)maxX, (float)maxY), 
			d = new Vec2f((float)minX, (float)maxY);
		
		return List.of(
				new Line2f(a, b),
				new Line2f(b, c),
				new Line2f(c, d),
				new Line2f(d, a)
				);
	}
}