package com.lying.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import net.minecraft.util.math.Vec2f;

public class LineUtils
{
	private static final List<BiFunction<Vec2f,Vec2f,ArrayList<Line2f>>> providers = List.of(
			LineUtils::xFirstCurved,
			LineUtils::yFirstCurved,
			LineUtils::xFirst,
			LineUtils::yFirst,
			LineUtils::diagonal
			);
	
	/** Attempts to generate a viable deterministic line, from the most elegant to the least */
	public static List<Line2f> trialLines(Vec2f start, Vec2f end, Predicate<List<Line2f>> qualifier)
	{
		ArrayList<Line2f> line = Lists.newArrayList();
		for(BiFunction<Vec2f,Vec2f,ArrayList<Line2f>> provider : providers)
		{
			line = provider.apply(start, end);
			line.removeIf(l -> l.length() == 0F);
			line.removeIf(Objects::isNull);
			
			if(line.isEmpty())
				continue;
			
			if(line.size() == 1 || qualifier.test(line))
				return line;
		}
		
		return line;
	}
	
	public static ArrayList<Line2f> diagonal(Vec2f start, Vec2f end)
	{
		return Lists.newArrayList(new Line2f(start, end));
	}
	
	public static ArrayList<Line2f> xFirst(Vec2f start, Vec2f end)
	{
		Vec2f offset = end.add(start.negate());
		Vec2f mid = start.add(new Vec2f(offset.x, 0F));
		return Lists.newArrayList(
				new Line2f(start, mid),
				new Line2f(mid, end)
				);
	}
	
	public static ArrayList<Line2f> yFirst(Vec2f start, Vec2f end)
	{
		Vec2f offset = end.add(start.negate());
		Vec2f mid = start.add(new Vec2f(0F, offset.y));
		return Lists.newArrayList(
				new Line2f(start, mid),
				new Line2f(mid, end)
				);
	}
	
	public static ArrayList<Line2f> xFirstCurved(Vec2f start, Vec2f end)
	{
		Line2f direct = new Line2f(start, end);
		if(Math.abs(direct.m) != 1)
			return Lists.newArrayList();
		
		ArrayList<Line2f> lines = Lists.newArrayList();
		
		Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
		Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
		
		Vec2f a = start.add(toX);
		Vec2f b = end.add(toY.negate());
		lines.add(new Line2f(start, a));
		lines.add(new Line2f(a, b));
		lines.add(new Line2f(b, end));
		return lines;
	}
	
	public static ArrayList<Line2f> yFirstCurved(Vec2f start, Vec2f end)
	{
		Line2f direct = new Line2f(start, end);
		if(Math.abs(direct.m) != 1)
			return Lists.newArrayList();
		
		ArrayList<Line2f> lines = Lists.newArrayList();
		
		Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
		Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
		
		Vec2f a = start.add(toY);
		Vec2f b = end.add(toX.negate());
		lines.add(new Line2f(start, a));
		lines.add(new Line2f(a, b));
		lines.add(new Line2f(b, end));
		return lines;
	}
}