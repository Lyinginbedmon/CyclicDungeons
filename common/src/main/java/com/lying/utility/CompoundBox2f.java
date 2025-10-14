package com.lying.utility;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.util.math.Vec2f;

public class CompoundBox2f extends AbstractBox2f
{
	private List<AbstractBox2f> boxes = Lists.newArrayList();
	
	public String toString() { return "CompBox["+boxes.size()+"]"; }
	
	public CompoundBox2f add(AbstractBox2f box)
	{
		boxes.add(box);
		return this;
	}
	
	public float minX()
	{
		float minX = Float.MAX_VALUE;
		for(AbstractBox2f b : boxes)
			if(b.minX() < minX)
				minX = b.minX();
		return minX;
	}
	
	public float minY()
	{
		float minY = Float.MAX_VALUE;
		for(AbstractBox2f b : boxes)
			if(b.minY() < minY)
				minY = b.minY();
		return minY;
	}
	
	public float maxX()
	{
		float maxX = Float.MIN_VALUE;
		for(AbstractBox2f b : boxes)
			if(b.maxX() > maxX)
				maxX = b.maxX();
		return maxX;
	}
	
	public float maxY()
	{
		float maxY = Float.MIN_VALUE;
		for(AbstractBox2f b : boxes)
			if(b.maxY() > maxY)
				maxY = b.maxY();
		return maxY;
	}
	
	public boolean contains(Vec2f vec)
	{
		return boxes.stream().anyMatch(b -> b.contains(vec));
	}
	
	public List<LineSegment2f> asEdges()
	{
		List<LineSegment2f> edges = Lists.newArrayList();
		boxes.forEach(b -> edges.addAll(b.asEdges()));
		return edges;
	}
	
	public List<Vec2f> asPoints()
	{
		List<Vec2f> points = Lists.newArrayList();
		boxes.stream().map(AbstractBox2f::asPoints).forEach(points::addAll);
		return points;
	}
	
	public List<AbstractBox2f> asBoxes()
	{
		return boxes;
	}
	
	public AbstractBox2f move(Vec2f vec)
	{
		CompoundBox2f box = new CompoundBox2f();
		boxes.stream().map(b -> b.move(vec)).forEach(box::add);
		return box;
	}
	
	public AbstractBox2f mul(float val)
	{
		CompoundBox2f box = new CompoundBox2f();
		boxes.stream().map(b -> b.mul(val)).forEach(box::add);
		return box;
	}
	
	public AbstractBox2f grow(float val)
	{
		CompoundBox2f box = new CompoundBox2f();
		boxes.stream().map(b -> b.grow(val)).forEach(box::add);
		return box;
	}
}
