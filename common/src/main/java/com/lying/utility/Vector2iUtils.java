package com.lying.utility;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import net.minecraft.util.math.Vec2f;

public class Vector2iUtils
{
	public static Vec2f toVec2f(Vector2i vec) { return new Vec2f(vec.x, vec.y); }
	
	public static Vector2i add(Vector2i a, Vector2i b)
	{
		return new Vector2i(a.x + b.x, a.y + b.y);
	}
	
	public static Vector2i subtract(Vector2i val, Vector2i from)
	{
		return new Vector2i(from.x - val.x, from.y - val.y);
	}
	
	public static Vector2i mul(Vector2i a, double scalar)
	{
		return new Vector2i((int)(a.x * scalar), (int)(a.y * scalar));
	}
	
	public static Vector2i normalize(Vector2i a)
	{
		return mul(a, 1 / a.length());
	}
	
	public static Vector2i negate(Vector2i a)
	{
		return mul(a, -1);
	}
	
	@Nullable
	public static Vector2i avg(Vector2i... values)
	{
		if(values.length == 0)
			return null;
		else if(values.length == 1)
			return values[0];
		
		int x = 0, y = 0;
		for(Vector2i vec : values)
		{
			x += vec.x;
			y += vec.y;
		}
		return new Vector2i(x / values.length, y / values.length);
	}
	
	public static Vector2i rotate(Vector2i a, double radians)
	{
		if(radians == Math.toRadians(90D))
			return new Vector2i(-a.y(), a.x());
		
		final double cos = Math.cos(radians), sin = Math.sin(radians);
		return new Vector2i(
				(int)(a.x() * cos - a.y() * sin),
				(int)(a.x() * sin + a.y() * cos)
				);
	}
}