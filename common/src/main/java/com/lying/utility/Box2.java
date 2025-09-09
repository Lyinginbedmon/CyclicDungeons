package com.lying.utility;

import org.joml.Vector2i;

import net.minecraft.util.math.Box;

public class Box2 extends Box
{
	public Box2(int minX, int maxX, int minY, int maxY)
	{
		super(minX, minY, 0, maxX, maxY, 1);
	}
	
	public Box2 offset(Vector2i vec)
	{
		return offset(vec.x, vec.y);
	}
	
	public Box2 offset(int x, int y)
	{
		return new Box2((int)minX + x, (int)maxX + x, (int)minY + y, (int)maxY + y);
	}
}