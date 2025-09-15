package com.lying.utility;

import org.joml.Vector2i;

public class Box2
{
	private int minX, minY, maxX, maxY;
	
	public Box2(int minX, int maxX, int minY, int maxY)
	{
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}
	
	public Box2 offset(Vector2i vec)
	{
		return offset(vec.x, vec.y);
	}
	
	public Box2 offset(int x, int y)
	{
		return new Box2((int)minX + x, (int)maxX + x, (int)minY + y, (int)maxY + y);
	}
	
	public Box2 scale(int scalar)
	{
		return new Box2(minX * scalar, maxX * scalar, minY * scalar, maxY * scalar);
	}
	
	public boolean intersects(Box2 box)
	{
		return intersects(box.minX, box.minY, box.maxX, box.maxY);
	}
	
	public boolean intersects(int minX, int minY, int maxX, int maxY)
	{
		return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY;
	}
	
	public boolean intersects(Line2 line)
	{
		// If the bounds contain either point of a line, it must intersect
		if(contains(line.getLeft()) || contains(line.getRight()))
			return true;
		
		// If the line intersects any boundary line of the bounds, it must intersect
		Vector2i 
			a = new Vector2i(minX, minY), 
			b = new Vector2i(maxX, minY), 
			c = new Vector2i(maxX, maxY), 
			d = new Vector2i(minX, maxY);
		
		return 
				line.intersects(new Line2(a, b)) ||
				line.intersects(new Line2(b, c)) ||
				line.intersects(new Line2(c, d)) ||
				line.intersects(new Line2(d, a));
	}
	
	public boolean contains(Vector2i vec)
	{
		return vec.x > minX && vec.x < maxX && vec.y > minY && vec.y < maxY;
	}
}