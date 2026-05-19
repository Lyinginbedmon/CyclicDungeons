package com.lying.graph;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.Blueprint.ErrorType;
import com.lying.grid.GridTile;
import com.lying.init.CDLoggers;
import com.lying.utility.CDUtils;
import com.lying.utility.logging.DataLog;
import com.lying.utility.logging.DebugLogger;

/** Utility class for reducing the footprint of a blueprint */
public class GraphScruncher
{
	public static DebugLogger LOGGER = CDLoggers.PLANAR;
	public static DataLog DATA_LOG = new DataLog();
	
	/**
	 * Applies scrunch algorithm until failure
	 * @param chart The blueprint to collapse
	 */
	public static void collapse(Blueprint chart)
	{
		final int iterationCap = chart.theme().collapseIterationCap();
		DATA_LOG.info("Applying collapse to {} nodes, cap {}", chart.size(), iterationCap);
		int iterations = iterationCap;
		final long time = System.currentTimeMillis();
		while(scrunch(chart) && iterations-- > 0)
			;
		
		DATA_LOG.info(" # Time to complete collapse operation: {}ms over {} iterations", System.currentTimeMillis() - time, iterationCap - iterations);
	}
	
	/**
	 * Reduces the distance between nodes, reducing passageway length
	 * @param chart The blueprint to scrunch
	 * @return True if any node was successfully moved
	 */
	public static boolean scrunch(Blueprint chart)
	{
		DATA_LOG.clear();
		DATA_LOG.info("Applying scrunch to {} nodes", chart.size());
		final long time = System.currentTimeMillis();
		boolean anyMoved = false;
		for(int i=1; i <= chart.maxDepth(); i++)
		{
			List<BlueprintRoom> nodes = chart.byDepth(i);
			DATA_LOG.info(" - Scrunching {} nodes at depth {}", nodes.size(), i);
			if(tryScrunch(nodes, chart))
				anyMoved = true;
		}
		DATA_LOG.info("Scrunch operation completed");
		DATA_LOG.info(" # Time to complete scrunch operation: {}ms", System.currentTimeMillis() - time);
		return anyMoved;
	}
	
	private static boolean tryScrunch(List<BlueprintRoom> nodes, Blueprint chart)
	{
		boolean anyMoved = false;
		for(BlueprintRoom node : nodes)
			if(tryScrunchNode(node, chart))
				anyMoved = true;
		return anyMoved;
	}
	
	/** Attempts to reduce the distance between the node and its parents */
	public static boolean tryScrunchNode(BlueprintRoom node, Blueprint chart)
	{
		DATA_LOG.info(" -- Processing {}", node);
		
		// Find the passage that opens into this room
		Optional<BlueprintPassage> entryPassage = Blueprint.getPassageInto(node, chart);
		if(entryPassage.isEmpty())
		{
			DATA_LOG.warn(" ? Couldn't find passage leading to {}", node);
			return false;
		}
		
		final BlueprintPassage passage = entryPassage.get();
		if(passage.containsFailures())
			return false;
		
		// Identify the start of the passage and its end at this room
		GridTile 
			start = passage.getInitialTile(), 
			end = node.getEntryTile();
		if(start == null || end == null || start.equals(end))
		{
			DATA_LOG.info(" = No movement needed");
			return false;
		}
		
		// Calculate direction towards start from end, then convert axial values to magnitude=1
		Vector2i vec = start.toVec2i().sub(end.toVec2i());
		vec = new Vector2i((int)Math.signum(vec.x), (int)Math.signum(vec.y));
		return tryMoveRelative(node, chart, vec);
	}
	
	/**
	 * Attempts to move the given room by the given vector
	 * @param node The room to be moved
	 * @param chart The blueprint the room exists within
	 * @param move The local vector to apply to the room
	 * @return True if the room was successfully moved at all
	 */
	private static boolean tryMoveRelative(BlueprintRoom node, Blueprint chart, Vector2i move)
	{
		if(move.length() == 0)
			return false;
		
		// Try move the full offset
		if(tryMove(node, chart, move))
			return true;
		
		// If both axises are non-zero, try to move on each individually
		else if(move.x != 0 && move.y != 0)
		{
			Vector2i onlyX = new Vector2i(move.x, 0);
			Vector2i onlyY = new Vector2i(0, move.y);
			
			Supplier<Boolean> tryX = () -> tryMove(node, chart, onlyX);
			Supplier<Boolean> tryY = () -> tryMove(node, chart, onlyY);
			
			// Prioritise moving in whichever direction results in the shortest distance to the point
			if(Math.abs(move.x) >= Math.abs(move.y))
				return tryX.get() ? true : tryY.get();
			else
				return tryY.get() ? true : tryX.get();
		}
		
		return false;
	}
	
	/**
	 * Simulates the move, then applies it if it does not produce an error in the blueprint
	 * @param node The room to be moved
	 * @param chart The blueprint the room exists within
	 * @param move The local vector to apply to the room
	 * @return True if the move would not produce an error in the blueprint and was applied
	 */
	public static boolean tryMove(BlueprintRoom node, Blueprint chart, Vector2i move)
	{
		// Clone blueprint and simulate movement
		// Apply movement to all descendants as well to minimise overall processing time
		Blueprint sim = chart.clone();
		List<BlueprintRoom> simNodes = Lists.newArrayList();
		simNodes.add(sim.getRoom(node.uuid()).get());
		simNodes.addAll(BlueprintRoom.getDescendants(simNodes.getFirst(), sim));
		
		simNodes.forEach(n -> n.move(move));
		for(ErrorType error : ErrorType.values())
			if(error.anyExist(sim))
			{
				DATA_LOG.error(" ! {} error precluded movement of {} by {}", error.name(), node, CDUtils.formatVec2i(move));
				return false;
			}
		
		if(sim.passageTiles() > chart.passageTiles())
		{
			DATA_LOG.warn(" ? Movement of {} by {} increased overall passage footprint", node, CDUtils.formatVec2i(move));
			return false;
		}
		
		// If the simulation caused no errors, apply it to the live blueprint
		node.move(move);
		BlueprintRoom.getDescendants(node, chart).forEach(n -> n.move(move));
		return true;
	}
}
