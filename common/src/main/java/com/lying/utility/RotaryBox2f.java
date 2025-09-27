package com.lying.utility;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.util.math.Vec2f;

public class RotaryBox2f
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
	public Box2f simplify()
	{
		return new Box2f(minX, maxX, minY, maxY);
	}
	
	public Line2f[] edges()
	{
		Line2f[] set = new Line2f[4];
		for(int i=0; i<4; i++)
			set[i] = edges[i];
		return set;
	}
	
	/**
	 * Returns true if the given point is within the simple bounds of this box.<br>
	 * This does not implicitly mean it is within the box itself.
	 */
	public boolean inBounds(Vec2f vec)
	{
		return simplify().contains(vec);
	}
	
	public boolean contains(Vec2f vec)
	{
		// A singular point cannot possibly be within the box if it is outside the simplified bounds
		if(vec.x < minX || vec.x > maxX || vec.y < minY || vec.y > maxY)
			return false;
		
		// Connect point to infinity in each cardinal direction
		for(Vec2f test : new Vec2f[] { new Vec2f(0, 1), new Vec2f(1, 0) })
		{
			// If the number of intersections is odd in any direction, the point must be inside the box
			
			Line2f linePos = new Line2f(vec, vec.add(test.multiply(Float.MAX_VALUE)));
			if(intersections(linePos)%2 > 0)
				return true;
			
			Line2f lineNeg = new Line2f(vec, vec.add(test.multiply(Float.MIN_VALUE)));
			if(intersections(lineNeg)%2 > 0)
				return true;
		}
		
		return false;
	}
	
	public int intersections(Line2f line)
	{
		int tally = 0;
		for(Line2f edge : edges)
			if(edge.intersects(line))
				tally++;
		return tally;
	}
	
	public boolean intersects(Line2f line)
	{
		return intersections(line) > 0;
	}
	
	public boolean intersects(Box2f box)
	{
		if(simplify().intersects(box))
		{
			for(Vec2f point : points)
				if(box.contains(point))
					return true;
			
			for(Line2f edge : edges)
				if(box.intersects(edge))
					return true;
		}
		return false;
	}
	
	/** Offsets the box by the given vector */
	public RotaryBox2f move(Vec2f vec)
	{
		for(int i=0; i<4; i++)
			points[i] = vec.add(points[i]);
		updateValues();
		return this;
	}
	
	/** Rotates the box around its core position */
	public RotaryBox2f spin(float radians)
	{
		return rotateAround(new Vec2f(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2), radians);
	}
	
	/** Rotates the box around [0,0] */
	public RotaryBox2f rotate(float radians)
	{
		for(int i=0; i<4; i++)
			points[i] = CDUtils.rotate(points[i], radians);
		updateValues();
		return this;
	}
	
	/** Rotates the box around the given position */
	public RotaryBox2f rotateAround(Vec2f origin, float radians)
	{
		return move(origin.negate()).rotate(radians).move(origin);
	}
	
	/** Multiplies all values of the box by the given value */
	public RotaryBox2f mul(float val)
	{
		for(int i=0; i<4; i++)
			points[i] = points[i].multiply(val);
		updateValues();
		return this;
	}
	
	/** Scales the box around its core position */
	public RotaryBox2f scale(float val)
	{
		Vec2f origin = new Vec2f(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2);
		return move(origin.negate()).mul(val).move(origin);
	}
	
	/** Returns a list of all 2D positions enclosed by this box */
	public List<Vec2f> enclosedPositions()
	{
		List<Vec2f> points = Lists.newArrayList();
		Box2f simplified = simplify();
		for(float x=simplified.minX(); x<simplified.maxX(); x++)
			for(float y=simplified.minY(); y<simplified.maxY(); y++)
			{
				Vec2f point = new Vec2f(x,y);
				if(contains(point))
					points.add(point);
			}
		return points;
	}
}
