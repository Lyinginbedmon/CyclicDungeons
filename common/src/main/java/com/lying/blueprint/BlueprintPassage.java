package com.lying.blueprint;

import java.util.List;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.lying.init.CDTiles;
import com.lying.utility.Box2f;
import com.lying.utility.Line2f;
import com.lying.utility.RotaryBox2f;
import com.lying.worldgen.Tile;
import com.lying.worldgen.TileSet;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class BlueprintPassage
{
	public static final int PASSAGE_WIDTH = 3;
	private final Line2f line;
	private final RotaryBox2f box;
	
	public BlueprintPassage(Line2f lineIn)
	{
		this(lineIn, PASSAGE_WIDTH);
	}
	
	public BlueprintPassage(Line2f lineIn, float width)
	{
		line = lineIn;
		box = RotaryBox2f.fromLine(line, width);
	}
	
	public BlueprintPassage(Vec2f a, Vec2f b)
	{
		this(new Line2f(a, b));
	}
	
	public Line2f asLine() { return line; }
	
	public RotaryBox2f asBox() { return box; }
	
	public void build(BlockPos origin, ServerWorld world, List<Box2f> boundaries)
	{
		TileSet map = new TileSet();
		
		// Add enclosed positions to volume where they exceed the boundaries of rooms
		final Predicate<Vec2f> isExterior = p -> boundaries.stream().noneMatch(b -> b.contains(p));
		final List<Vec2f> positions = box.enclosedPositions();
		if(positions.stream().noneMatch(isExterior))
			return;
		
		positions.stream()
			.filter(isExterior)
			.forEach(p -> map.addToVolume(origin.add((int)p.x, 0, (int)p.y)));
		
		Vec2f parentPos = line.getLeft();
		Vec2f childPos = line.getRight();
		
		// Populate map
		Vec2f current = parentPos;
		while(current.distanceSquared(childPos) > 0)
		{
			double minDist = Double.MAX_VALUE;
			Direction face = Direction.NORTH;
			for(Direction facing : Direction.Type.HORIZONTAL)
			{
				double dist = current.add(new Vec2f(facing.getOffsetX(), facing.getOffsetZ())).distanceSquared(childPos);
				if(minDist > dist)
				{
					face = facing;
					minDist = dist;
				}
			}
			
			current = current.add(new Vec2f(face.getOffsetX(), face.getOffsetZ()));
			BlockPos pos = origin.add((int)current.x, 0, (int)current.y);
			if(map.contains(pos))
				map.put(pos, CDTiles.FLOOR.get());
			else
				Tile.tryPlace(Blocks.GRAY_CONCRETE_POWDER.getDefaultState(), pos, world);
		}
		
		// Generate
//		map.generate(world);
	}
	
	/** Returns true if the given point is either end of this passage */
	public boolean isTerminus(Vec2f point)
	{
		return line.getLeft().equals(point) || line.getRight().equals(point);
	}
	
	/** Returns true if the given room is either intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		Vector2i pos = room.position();
		return isTerminus(new Vec2f(pos.x, pos.y));
	}
	
	public boolean linksTo(BlueprintPassage b) { return isTerminus(b.line.getLeft()) || isTerminus(b.line.getRight()); }
	
	public boolean intersects(BlueprintPassage b) { return line.intersects(b.line); }
	
	/** Returns true if this passage intersects with any other passages in the given chart */
	public boolean hasIntersections(List<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Blueprint.getPassages(chart);
		return paths.stream()
				.filter(p -> !linksTo(p))
				.anyMatch(this::intersects);
	}
	
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean hasTunnels(List<BlueprintRoom> chart)
	{
		return chart.stream()
				.filter(r -> !isTerminus(r))
				.map(BlueprintRoom::bounds)
				.anyMatch(this.box::intersects);
	}
}
