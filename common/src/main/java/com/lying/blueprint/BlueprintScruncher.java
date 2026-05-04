package com.lying.blueprint;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.grid.GridTile;
import com.lying.init.CDLoggers;
import com.lying.utility.DebugLogger;

/** Utility class for reducing the footprint of a blueprint */
public class BlueprintScruncher
{
	public static DebugLogger LOGGER = CDLoggers.PLANAR;
	
	/**
	 * Applies scrunch algorithm until failure
	 * @param chart The blueprint to collapse
	 * @param reverse True if the collapse should start at the deepest nodes instead of the shallowest
	 */
	public static void collapse(Blueprint chart)
	{
		int iterations = 1000;
		final long time = System.currentTimeMillis();
		while(scrunch(chart) && iterations-- > 0)
			;
		
		CyclicDungeons.LOGGER.info(" # Time to complete collapse operation: {}ms over {} iterations", System.currentTimeMillis() - time, 1000 - iterations);
	}
	
	/**
	 * Reduces the distance between nodes, reducing passageway length
	 * @param chart The blueprint to scrunch
	 * @return
	 */
	public static boolean scrunch(Blueprint chart)
	{
		final long time = System.currentTimeMillis();
		boolean anyMoved = false;
		for(int i=chart.maxDepth(); i>0; i--)
			if(tryScrunch(chart.byDepth(Math.abs(i)), chart))
				anyMoved = true;
		
		CyclicDungeons.LOGGER.info(" # Time to complete scrunch operation: {}ms", System.currentTimeMillis() - time);
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
		// If we have no parents, we can't know where to move
		if(!node.hasParents())
			return false;
		
		/**
		 * Identify node entryway tile
		 * Identify passage containing entryway tile
		 * Calculate direction from entryway tile to passage start tile
		 * Try move in that direction
		 */
		final GridTile entryWay = node.getEntryTile();
		if(entryWay == null)
			return false;
		
		Optional<BlueprintPassage> entryPassage = Blueprint.getPassageInto(node, chart);
		if(entryPassage.isEmpty() || entryPassage.get().containsFailures())
			return false;
		
		GridTile startTile = entryPassage.get().getInitialTile().orElse(null);
		if(startTile != null && !startTile.equals(entryWay))
		{
			Vector2i direction = startTile.toVec2i().sub(entryWay.toVec2i());
			return tryMoveRelative(node, chart, new Vector2i((int)Math.signum(direction.x()), (int)Math.signum(direction.y)));
		}
		return false;
	}
	
	/**
	 * Attempts to move the given room by the given vector
	 * @param node The room to be moved
	 * @param chart The blueprint the room exists within
	 * @param move The local vector to apply to the room
	 * @return True if the room was successfully moved at all
	 */
	public static boolean tryMoveRelative(BlueprintRoom node, Blueprint chart, Vector2i move)
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
		if(move.length() == 0)
			return false;
		
		// Clone blueprint and simulate movement
		// Apply movement to all descendants as well to minimise overall processing time
		Blueprint sim = chart.clone();
		List<BlueprintRoom> simNodes = Lists.newArrayList();
		simNodes.add(sim.getRoom(node.uuid()).get());
		simNodes.addAll(BlueprintRoom.getDescendants(simNodes.getFirst(), sim));
		
		for(BlueprintRoom simNode : simNodes)
		{
			simNode.move(move);
			if(sim.hasErrors())
				return false;
		}
		
		// If the simulation caused no errors, apply it to the live blueprint
		node.move(move);
		BlueprintRoom.getDescendants(node, chart).forEach(n -> n.move(move));
		return true;
	}
}
