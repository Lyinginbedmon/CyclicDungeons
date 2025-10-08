package com.lying.utility;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import net.minecraft.util.math.Vec2f;

public class PolyPath
{
	private final Vec2f start;
	private final List<Vec2f> ends = Lists.newArrayList();
	
	public PolyPath(Vec2f startIn, Vec2f... endsIn)
	{
		start = startIn;
		for(Vec2f vec : endsIn)
			ends.add(vec);
	}
	
	public void addEnd(Vec2f end)
	{
		if(!ends.contains(end))
			ends.add(end);
	}
	
	public final List<Vec2f> points()
	{
		List<Vec2f> points = Lists.newArrayList();
		points.add(start);
		points.addAll(ends);
		return points;
	}
	
	public PolyPath offset(Vec2f pos)
	{
		PolyPath path = new PolyPath(start.add(pos));
		ends.stream().map(e -> e.add(pos)).forEach(path::addEnd);
		return path;
	}
	
	public final int size() { return 1 + ends.size(); }
	
	/** Returns the central focal point of all positions in this path */
	public Vec2f focalPoint()
	{
		float x = start.x, y = start.y;
		for(Vec2f vec : ends)
		{
			x += vec.x;
			y += vec.y;
		}
		
		x /= ends.size() + 1;
		y /= ends.size() + 1;
		return new Vec2f(x, y);
	}
	
	public List<Line2f> asLines()
	{
		return asLines(Predicates.alwaysTrue());
	}
	
	public List<Line2f> asLines(Predicate<List<Line2f>> qualifier)
	{
		Vec2f focus = focalPoint();
		List<Line2f> lines = Lists.newArrayList();
		for(Vec2f point : points())
			lines.addAll(LineUtils.trialLines(point, focus, qualifier));
		
		return lines;
	}
	
	public AbstractBox2f bounds()
	{
		float x = Float.MAX_VALUE, X = Float.MIN_VALUE;
		float y = Float.MAX_VALUE, Y = Float.MIN_VALUE;
		for(Vec2f vec : points())
		{
			if(vec.x > X)
				X = vec.x;
			if(vec.x < x)
				x = vec.x;
			
			if(vec.y > Y)
				Y = vec.y;
			if(vec.y < Y)
				y = vec.y;
		}
		return new Box2f(x,X,y,Y);
	}
}
