package com.lying.blueprint;

import java.util.List;

import org.joml.Vector2i;

import com.lying.utility.Line2;
import com.lying.utility.RotaryBox2;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlueprintPassage
{
	public static final int PASSAGE_WIDTH = 5;
	private final Line2 line;
	private final RotaryBox2 box;
	
	public BlueprintPassage(Line2 lineIn)
	{
		line = lineIn;
		box = RotaryBox2.fromLine(line, PASSAGE_WIDTH);
	}
	
	public BlueprintPassage(Vector2i a, Vector2i b)
	{
		this(new Line2(a, b));
	}
	
	public Line2 asLine() { return line; }
	
	public RotaryBox2 asBox() { return box; }

	// FIXME Restrict to only constructing outside room boundaries
	public void build(BlockPos origin, ServerWorld world)
	{
		Vector2i parentPos = line.getLeft();
		Vector2i childPos = line.getRight();
		BlockPos pos1 = origin.add(parentPos.x, 0, parentPos.y);
		BlockPos pos2 = origin.add(childPos.x, 0, childPos.y);
		
		BlockPos current = pos1;
		while(current.getSquaredDistance(pos2) > 0)
		{
			double minDist = Double.MAX_VALUE;
			Direction face = Direction.NORTH;
			for(Direction facing : Direction.Type.HORIZONTAL)
			{
				double dist = current.offset(facing).getSquaredDistance(pos2);
				if(minDist > dist)
				{
					face = facing;
					minDist = dist;
				}
			}
			
			Blueprint.tryPlaceAt(Blocks.SMOOTH_STONE.getDefaultState(), current, world);
			for(int i=2; i>0; i--)
				Blueprint.tryPlaceAt(Blocks.AIR.getDefaultState(), current.up(i), world);
			current = current.offset(face);
		}
	}
	
	/** Returns true if the given point is either end of this passage */
	public boolean isTerminus(Vector2i point)
	{
		return line.getLeft().equals(point) || line.getRight().equals(point);
	}
	
	/** Returns true if the given room is either intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		return isTerminus(room.position());
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
	
	// FIXME Resolve erroneous error reports
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean hasTunnels(List<BlueprintRoom> chart)
	{
		return chart.stream()
				.filter(r -> !isTerminus(r))
				.map(BlueprintRoom::bounds)
				.anyMatch(this.box::intersects);
	}
}
