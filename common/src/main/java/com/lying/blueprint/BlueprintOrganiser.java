package com.lying.blueprint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.utility.CDUtils;
import com.lying.utility.Line2;
import com.lying.utility.Vector2iUtils;

/** Utilities for organising a blueprint, prior to scrunching */
public abstract class BlueprintOrganiser
{
	private static final int GRID_SIZE = 50;
	
	public abstract void organise(Blueprint chart, Random rand);
	
	public static class Tree extends BlueprintOrganiser
	{
		public static Tree create() { return new Tree(); }
		
		public void organise(Blueprint chart, Random rand)
		{
			for(int depth=0; depth<=chart.maxDepth(); depth++)
			{
				List<BlueprintRoom> nodes = chart.byDepth(depth);
				int y = depth * GRID_SIZE;
				int rowWidth = (nodes.size() - 1) * GRID_SIZE;
				int x = -rowWidth / 2;
				for(BlueprintRoom node : nodes)
				{
					node.setPosition(x, y);
					x += GRID_SIZE;
				}
			}
		}
	}
	
	public static abstract class Grid extends BlueprintOrganiser
	{
		protected abstract GridPosition[] moveSet(Vector2i position);
		
		@FunctionalInterface
		public interface GridPosition
		{
			public Vector2i get(Vector2i position, Map<Vector2i,BlueprintRoom> occupancies, int gridSize);
		}
		
		public void organise(Blueprint chart, Random rand)
		{
			Map<Vector2i, BlueprintRoom> gridMap = new HashMap<>();
			
			for(int step = 0; step <= chart.maxDepth(); step++)
				organiseByGrid(chart, step, gridMap, this::moveSet, GRID_SIZE, rand);
			
			if(gridMap.size() != chart.size())
				CyclicDungeons.LOGGER.warn("Grid layout size ({}) differs from input blueprint size ({})", gridMap.size(), chart.size());
		}
		
		private static void organiseByGrid(Blueprint chart, int depth, Map<Vector2i,BlueprintRoom> gridMap, Function<Vector2i,GridPosition[]> moveSet, int gridSize, Random rand)
		{
			for(BlueprintRoom node : chart.byDepth(depth))
				if(gridMap.isEmpty())
				{
					node.setPosition(0, 0);
					gridMap.put(node.position(), node);
				}
				else if(node.hasParents())
				{
					Vector2i position = new Vector2i(0,0);
					
					// Find unoccupied position adjacent to parent(s)
					List<BlueprintRoom> parents = node.getParents(chart);
					if(!parents.isEmpty())
					{
						List<Vector2i> options = getAvailableOptions(parents, node.childrenCount(), moveSet, gridSize, gridMap);
						if(options.isEmpty())
							continue;	// FIXME Resolve contexts where nodes have nowhere to go
						
						if(options.size() > 1)
						{
							// Prioritise viable position farthest from parents of parent nodes
							List<Vector2i> gPositions = Lists.newArrayList();
							parents.forEach(n -> gPositions.add(n.getParentPosition(chart)));
							if(!gPositions.isEmpty())
							{
								Vector2i gPos = Vector2iUtils.avg(gPositions.toArray(new Vector2i[0]));
								double maxDist = -1;
								Map<Vector2i, Double> distMap = new HashMap<>();
								for(Vector2i option : options)
								{
									double dist = option.distance(gPos);
									distMap.put(option, dist);
									if(dist > maxDist)
										maxDist = dist;
								}
								
								final double optimalDist = maxDist;
								List<Pair<Vector2i,Float>> weights = Lists.newArrayList();
								distMap.entrySet().forEach(e -> weights.add(Pair.of(e.getKey(), (float)(e.getValue() / optimalDist))));
								
								position = CDUtils.selectFromWeightedList(weights, rand.nextFloat());
							}
							else
								position = options.get(rand.nextInt(options.size()));
						}
						else
							position = options.get(0);
					}
					
					node.setPosition(position.x, position.y);
					gridMap.put(position, node);
				}
		}
		
		public static List<Vector2i> getAvailableOptions(List<BlueprintRoom> parents, int childTally, Function<Vector2i, GridPosition[]> moveSet, int gridSize, Map<Vector2i,BlueprintRoom> gridMap)
		{
			List<Vector2i> options = Lists.newArrayList();
			for(BlueprintRoom parent : parents)
				getAvailableOptions(parent.position(), childTally, moveSet, gridSize, gridMap).stream().filter(p -> !options.contains(p)).forEach(options::add);
			
			// Make list of all existing paths in gridMap
			List<Line2> existingPaths = Blueprint.getAllPaths(gridMap.values());
			
			if(!existingPaths.isEmpty())
				options.removeIf(pos -> 
				{
					for(BlueprintRoom parent : parents)
					{
						// Test if the path intersects with any existing path in the grid
						final Line2 a = new Line2(pos, parent.position());
						if(existingPaths.stream().anyMatch(p -> a.intersects(p)))
							return true;
					}
					
					return false;
				});
			
			return options;
		}
		
		public static List<Vector2i> getAvailableOptions(Vector2i position, int minExits, Function<Vector2i,GridPosition[]> moveSet, int gridSize, Map<Vector2i,BlueprintRoom> gridMap)
		{
			List<Vector2i> options = Lists.newArrayList();
			for(GridPosition offset : moveSet.apply(position))
			{
				Vector2i neighbour = offset.get(position, gridMap, gridSize);
				if(gridMap.keySet().stream().noneMatch(neighbour::equals))
				{
					// Ensure the position has at least as many moves itself as the node has children
					if(minExits > 0 && getAvailableOptions(neighbour, -1, moveSet, gridSize, gridMap).size() < minExits)
						continue;
					
					options.add(neighbour);
				}
			}
			return options;
		}
		
		protected static class ScaledGridPosition implements GridPosition
		{
			private final double scale;
			private final int x, y;
			
			protected ScaledGridPosition(int xIn, int yIn, double scaleIn)
			{
				scale = scaleIn;
				x = xIn;
				y = yIn;
			}
			
			public static GridPosition of(double val, int x, int y) { return new ScaledGridPosition(x, y, val); }
			
			public Vector2i get(Vector2i position, Map<Vector2i,BlueprintRoom> occupancies, int gridSize)
			{
				int size = (int)((double)gridSize * scale);
				return Vector2iUtils.add(position, new Vector2i(x * size, y * size));
			}
		}
		
		public static class Square extends Grid
		{
			public static final GridPosition[] QUAD_GRID = new GridPosition[]
					{
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(g, 0)),
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(-g, 0)),
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(0, g)),
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(0, -g))
					};
			
			public static Square create() { return new Square(); }
			
			protected GridPosition[] moveSet(Vector2i position) { return QUAD_GRID; }
		}
		
		public static class Octagonal extends Grid
		{
			public static final GridPosition[] OCT_GRID_A = new GridPosition[]
					{
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(g, 0)),
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(-g, 0)),
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(0, g)),
						(p,o,g) -> Vector2iUtils.add(p, new Vector2i(0, -g)),
						
						ScaledGridPosition.of(0.5D, 1, 1),
						ScaledGridPosition.of(0.5D, 1, -1),
						ScaledGridPosition.of(0.5D, -1, 1),
						ScaledGridPosition.of(0.5D, -1, -1)
					};
			public static final GridPosition[] OCT_GRID_B = new GridPosition[]
					{
						ScaledGridPosition.of(0.5D, 1, 1),
						ScaledGridPosition.of(0.5D, 1, -1),
						ScaledGridPosition.of(0.5D, -1, 1),
						ScaledGridPosition.of(0.5D, -1, -1)
					};
			
			public static Octagonal create() { return new Octagonal(); }
			
			protected GridPosition[] moveSet(Vector2i position)
			{
				return position.y%1 == 0 ? OCT_GRID_A : OCT_GRID_B;
			}
		}
	}
}
