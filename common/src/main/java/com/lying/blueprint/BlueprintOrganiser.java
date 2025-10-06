package com.lying.blueprint;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.utility.CDUtils;
import com.lying.utility.DebugLogger;
import com.lying.utility.Vector2iUtils;

import net.minecraft.util.math.random.Random;

/** Utilities for organising a blueprint, prior to scrunching */
public abstract class BlueprintOrganiser
{
	public static final DebugLogger LOGGER = CDLoggers.PLANAR;
	private static final int GRID_SIZE = 30;
	
	public static final Comparator<BlueprintRoom> descendantSort(List<BlueprintRoom> chart)
	{
		return (a,b) -> 
		{
			int desA = a.descendantCount(chart);
			int desB = b.descendantCount(chart);
			return desA > desB ? -1 : desA < desB ? 1 : 0;
		};
	}
	
	public final void organise(Blueprint chart, Random rand)
	{
		LOGGER.info(" # Applying organiser to {}:{} planar graph", chart.size(), chart.maxDepth());
		
		// Clears all preceding structural information
		chart.forEach(n -> n.setPosition(0, 0));
		
		applyLayout(chart, rand);
	}
	
	public abstract void applyLayout(Blueprint chart, Random rand);
	
	public static class Tree extends BlueprintOrganiser
	{
		public static Tree create() { return new Tree(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			final Comparator<BlueprintRoom> descSort = descendantSort(chart);
			for(int depth=0; depth<=chart.maxDepth(); depth++)
			{
				List<BlueprintRoom> nodes = Lists.newArrayList();
				nodes.addAll(chart.byDepth(depth));
				nodes.sort(descSort);
				
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
	
	public static class Circular extends BlueprintOrganiser
	{
		public static Circular create() { return new Circular(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			final Comparator<BlueprintRoom> descSort = descendantSort(chart);
			for(int step = 1; step <= chart.maxDepth(); step++)
			{
				// Rooms at this radius
				List<BlueprintRoom> set = Lists.newArrayList();
				set.addAll(chart.byDepth(step));
				set.sort(descSort);
				
				// Calculate all slots available at this radius
				int radius = step * GRID_SIZE;
				double circ = 2 * (Math.PI * radius);
				
				/**
				 *  Number of slots = circumference / (grid size * 2)
				 *  eg. assuming grid size = 30: 3, 6, 9, 12, 15, 18, etc.
				 */
				int pop = Math.max((int)(circ / (GRID_SIZE * 2)), set.size());
				double rot = Math.toRadians(180D / pop);
				double cos = Math.cos(rot), sin = Math.sin(rot);
				
				Vector2i point = new Vector2i(radius, 0);
				List<Vector2i> positions = Lists.newArrayList();
				for(int i=0; i<pop; i++)
				{
					positions.add(point);
					point = new Vector2i(
							(int)(point.x() * cos - point.y() * sin),
							(int)(point.x() * sin + point.y() * cos)
							);
				}
				
				// Place all rooms in positions closest to their parents at their radius
				for(BlueprintRoom room : set)
				{
					if(positions.isEmpty())
						break;
					
					Vector2i parent = room.getParentPosition(chart);
					positions.sort((a,b) -> 
					{
						double dA = a.distance(parent);
						double dB = b.distance(parent);
						if(dA == dB)
						{
							int mA = Math.abs(a.x()) + Math.abs(a.y());
							int mB = Math.abs(b.x()) + Math.abs(b.y());
							if(mA == mB)
							{
								int yA = Math.abs(a.y());
								int yB = Math.abs(b.y());
								return yA > yB ? -1 : yA < yB ? 1 : 0;
							}
							
							return mA < mB ? -1 : mA > mB ? 1 : 0;
						}
						else
							return dA < dB ? -1 : dA > dB ? 1 : 0;
					});
					
					room.setPosition(positions.removeFirst());
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
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			Map<Vector2i, BlueprintRoom> gridMap = new HashMap<>();;
			
			for(int step = 0; step <= chart.maxDepth(); step++)
				organiseByGrid(chart, step, gridMap, this::moveSet, GRID_SIZE, rand);
			
			if(gridMap.size() != chart.size())
				LOGGER.warn("Grid layout size ({}) differs from input blueprint size ({})", gridMap.size(), chart.size());
		}
		
		private static void organiseByGrid(Blueprint chart, int depth, Map<Vector2i,BlueprintRoom> gridMap, Function<Vector2i,GridPosition[]> moveSet, int gridSize, Random rand)
		{
			List<BlueprintRoom> rooms = Lists.newArrayList();
			
			// Sort by descendant count so the nodes with the most descendants get assigned the clearest positions
			rooms.addAll(chart.byDepth(depth));
			rooms.sort(descendantSort(chart));
			
			for(BlueprintRoom node : rooms)
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
						
						position = selectOption(options, parents, chart, rand);
					}
					
					gridMap.put(position, node.setPosition(position));
				}
		}
		
		private static Vector2i selectOption(List<Vector2i> options, List<BlueprintRoom> parents, Blueprint chart, Random rand)
		{
			if(options.size() > 1)
			{
				// FIXME Moderate distance function to reduce influence as nodes get farther from dungeon start
//				Function<Vector2i, Double> distFunc = v -> 
//				{
//					double dist = 0D;
//					for(BlueprintRoom room : chart)
//						dist += Math.abs(v.x() - room.position().x()) + Math.abs(v.y() - room.position().y());
//					return dist / chart.size();
//				};
//				options.sort((a,b) -> 
//				{
//					double dA = distFunc.apply(a);
//					double dB = distFunc.apply(b);
//					return dA > dB ? -1 : dA < dB ? 1 : 0;
//				});
//				return options.get(0);
				
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
					
					return CDUtils.selectFromWeightedList(weights, rand.nextFloat());
				}
				else
					return options.get(rand.nextInt(options.size()));
			}
			else
				return options.get(0);
		}
		
		public static List<Vector2i> getAvailableOptions(List<BlueprintRoom> parents, int childTally, Function<Vector2i, GridPosition[]> moveSet, int gridSize, Map<Vector2i,BlueprintRoom> gridMap)
		{
			List<Vector2i> options = Lists.newArrayList();
			for(BlueprintRoom parent : parents)
				getAvailableOptions(parent.position(), childTally, moveSet, gridSize, gridMap).stream().filter(p -> !options.contains(p)).forEach(options::add);
			
			// Copy of the blueprint presently represented by the gridMap's values
			List<BlueprintRoom> posit = Lists.newArrayList();
			posit.addAll(gridMap.values().stream().map(BlueprintRoom::clone).toList());
			
			// Hypothetical room we are trying to place, with the same parent nodes
			BlueprintRoom concept = BlueprintRoom.create();
			parents.stream().forEach(p -> posit.stream().filter(pH -> pH.uuid().equals(p.uuid())).forEach(pH -> pH.addChild(concept)));
			posit.add(concept);
			
			options.removeIf(pos -> 
			{
				concept.setPosition(pos);
				return Blueprint.hasErrors(posit);
			});
			
			return options;
		}
		
		public static List<Vector2i> getAvailableOptions(Vector2i position, int minExits, Function<Vector2i,GridPosition[]> moveSet, int gridSize, Map<Vector2i,BlueprintRoom> gridMap)
		{
			List<Vector2i> options = Lists.newArrayList();
			for(GridPosition offset : moveSet.apply(position))
			{
				Vector2i neighbour = offset.get(position, gridMap, gridSize);
				if(!gridMap.containsKey(neighbour))
				{
					// Ensure the position has at least as many moves itself as the node has children
					if(minExits > 0 && getAvailableOptions(neighbour, -1, moveSet, gridSize, gridMap).size() <= minExits)
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
