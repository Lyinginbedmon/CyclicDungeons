package com.lying.utility;

import java.util.List;

import net.minecraft.util.math.Vec2f;

public class RotaryBox2f extends AbstractBox2f
{
	private Vec2f[] points = new Vec2f[4];
	private Line2f[] edges = new Line2f[4];
	
	private float minX, minY, maxX, maxY;
	
	public RotaryBox2f(Vec2f aIn, Vec2f bIn, Vec2f cIn, Vec2f dIn)
	{
		points[0] = aIn;
		points[1] = bIn;
		points[2] = cIn;
		points[3] = dIn;
		updateValues();
	}
	
	public String toString() { return "RotBox["+minX+"->"+maxX+", "+minY+"->"+maxY+"]"; }
	
	public RotaryBox2f clone()
	{
		return new RotaryBox2f(points[0], points[1], points[2], points[3]);
	}
	
	public void updateValues()
	{
		calculateSimpleBounds();
		buildEdges();
	}
	
	protected void calculateSimpleBounds()
	{
		float mx = Integer.MAX_VALUE, my = Integer.MAX_VALUE, mX = Integer.MIN_VALUE, mY = Integer.MIN_VALUE;
		for(Vec2f vec : points)
		{
			float x = vec.x;
			if(x > mX)
				mX = x;
			if(x < mx)
				mx = x;
			
			float y = vec.y;
			if(y > mY)
				mY = y;
			if(y < my)
				my = y;
		}
		minX = mx;
		minY = my;
		maxX = mX;
		maxY = mY;
	}
	
	public float minX() { return minX; }
	public float minY() { return minY; }
	public float maxX() { return maxX; }
	public float maxY() { return maxY; }
	
	protected void buildEdges()
	{
		edges = new Line2f[] 
				{
					new Line2f(points[0], points[1]), 
					new Line2f(points[1], points[2]), 
					new Line2f(points[2], points[3]), 
					new Line2f(points[3], points[0])
				};
	}
	
	public static RotaryBox2f enclosing(Vec2f a, Vec2f b)
	{
		float minX = Math.min(a.x, b.x);
		float minY = Math.min(a.y, b.y);
		float maxX = Math.max(a.x, b.x);
		float maxY = Math.max(a.y, b.y);
		return new RotaryBox2f(new Vec2f(minX, minY), new Vec2f(maxX, minY), new Vec2f(maxX, maxY), new Vec2f(minX, maxY));
	}
	
	public static RotaryBox2f ofSize(int width, int height)
	{
		return new RotaryBox2f(new Vec2f(0, 0), new Vec2f(width, 0), new Vec2f(width, height), new Vec2f(0, height));
	}
	
	public static RotaryBox2f fromLine(Line2f line, float height)
	{
		Vec2f p1 = new Vec2f(line.getLeft().x, line.getLeft().y);
		Vec2f p2 = new Vec2f(line.getRight().x, line.getRight().y);
		
		Vec2f delta = p2.add(p1.negate()).normalize();
		delta = new Vec2f(-delta.y, delta.x);
		float minH = -height / 2;
		float maxH = minH + height;
		
		Vec2f a = p1.add(delta.multiply(minH));
		Vec2f b = p2.add(delta.multiply(minH));
		Vec2f c = p2.add(delta.multiply(maxH));
		Vec2f d = p1.add(delta.multiply(maxH));
		
		return new RotaryBox2f(
				new Vec2f(a.x, a.y),
				new Vec2f(b.x, b.y),
				new Vec2f(c.x, c.y),
				new Vec2f(d.x, d.y)
				);
	}
	
	/** Reduces this box to its grid-aligned equivalent */
	public AbstractBox2f simplify()
	{
		return new Box2f(minX, maxX, minY, maxY);
	}
	
	public List<Line2f> asEdges()
	{
		return List.of(
				edges[0],
				edges[1],
				edges[2],
				edges[3]
				);
	}
	
	public List<Vec2f> asPoints()
	{
		return List.of(
				points[0],
				points[1],
				points[2],
				points[3]
				);
	}
	
	/** Offsets the box by the given vector */
	public AbstractBox2f move(Vec2f vec)
	{
		for(int i=0; i<4; i++)
			points[i] = vec.add(points[i]);
		updateValues();
		return this;
	}
	
	/** Multiplies all values of the box by the given value */
	public AbstractBox2f mul(float val)
	{
		for(int i=0; i<4; i++)
			points[i] = points[i].multiply(val);
		updateValues();
		return this;
	}
	
	/** Rotates the box around its core position */
	public AbstractBox2f spin(float radians)
	{
		return rotateAround(new Vec2f(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2), radians);
	}
	
	/** Rotates the box around [0,0] */
	public AbstractBox2f rotate(float radians)
	{
		for(int i=0; i<4; i++)
			points[i] = CDUtils.rotate(points[i], radians);
		updateValues();
		return this;
	}
	
	/** Rotates the box around the given position */
	public AbstractBox2f rotateAround(Vec2f origin, float radians)
	{
		return move(origin.negate()).rotate(radians).move(origin);
	}
	
	public AbstractBox2f grow(float val)
	{
		return this;
	}
}
