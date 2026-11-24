package com.lying.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;
import org.slf4j.Logger;

import com.lying.CyclicDungeons;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintRoom;
import com.lying.init.CDTerms;

public class GravityScrunch
{
	/** Minimum number of tiles between adjacent unconnected room grids */
	private static final int MIN_SEP	= 1;
	
	private static final Logger LOGGER = CyclicDungeons.LOGGER;
	
	public static void collapse(Blueprint chart)
	{
		scrunch(chart, 100);
	}
	
	public static void scrunch(Blueprint chart)
	{
		scrunch(chart, -1);
	}
	
	public static void scrunch(Blueprint chart, final int cap)
	{
		// Map of rooms to the functions calculating their motion
		Map<BlueprintRoom, VerletSolver> verletObjects = new HashMap<>();
		
		// Populate gravities
		for(BlueprintRoom room : chart)
		{
			// Starting rooms are unaffected by gravitational forces
			if(room.metadata().is(CDTerms.instance().start()))
				continue;
			
			Optional<BlueprintRoom> parent = Optional.empty();
			for(BlueprintRoom r : chart)
				if(r.equals(room))
					continue;
				else if(r.isChild(room) && parent.isEmpty())
				{
					parent = Optional.of(r);
					break;
				}
			
			verletObjects.put(room, new VerletSolver(parent));
		}
		
		// Assemble measure of forces acting on each room in its current position
		final Map<Integer, List<BlueprintRoom>> byDepth = new HashMap<>();
		chart.forEach(room -> 
		{
			final int depth = room.metadata().depth();
			List<BlueprintRoom> set = byDepth.getOrDefault(depth, new ArrayList<>());
			set.add(room);
			byDepth.put(depth, set);
		});
		/** Map of depth to all colliders at that depth */
		final Map<Integer, List<BlueprintRoom>> depthColliders = new HashMap<>();
		for(int d : byDepth.keySet())
		{
			List<BlueprintRoom> rooms = new ArrayList<>();
			for(int i=0; i<=d; i++)
				rooms.addAll(byDepth.get(i));
			depthColliders.put(d, rooms);
		}
		
		int limit = cap;
		boolean changed = false;
		do
		{
			for(int i=1; i<chart.maxDepth(); i++)
				for(BlueprintRoom room : byDepth.get(i))
					changed = verletObjects.get(room).applyTo(room, depthColliders.get(i)) || changed;
		}
		while(limit-- > 0 && changed);
	}
	
	/** Essentially a Verlet integration object for each room */
	private static record VerletSolver(Optional<BlueprintRoom> gravityOrigin)
	{
		public boolean applyTo(BlueprintRoom room, List<BlueprintRoom> colliders)
		{
			if(gravityOrigin.isEmpty())
				return false;
			
			final Vector2i currentPos = room.tilePosition().toVec2i();
			final Vector2i parentPos = gravityOrigin.get().tilePosition().toVec2i();
			final Vector2i direction = new Vector2i(parentPos).sub(currentPos);
			
			final Vector2i prev = new Vector2i(currentPos);
			applyVelocity(room, new Vector2i(
					(int)Math.signum(direction.x), 
					(int)Math.signum(direction.y)
					), colliders);
			return prev.gridDistance(room.tilePosition().toVec2i()) > 0;
		}
		
		private static void applyVelocity(BlueprintRoom room, Vector2i acceleration, List<BlueprintRoom> colliders)
		{
			if(acceleration.length() == 0)
				return;
			
			// Move towards parent room
			room.move(acceleration);
			
			// Occlude rooms moving ahead of the starting room (occupying any tile with Y < 0)
			final int lowestY = (int)Math.ceil(room.metadata().tileSize().y * 0.5D);
			if(room.tilePosition().toVec2i().y < lowestY)
				room.move(new Vector2i(
						0, 
						lowestY - room.tilePosition().toVec2i().y)
						);
			
			// Occlude rooms from occupying each-other's space
			for(BlueprintRoom other : colliders)
			{
				if(room.equals(other))
					continue;
				
				// FIXME Refactor to treat rooms as actual squares instead of circles
				
				final Vector2i aPos = room.tilePosition().toVec2i();
				final Vector2i bPos = other.tilePosition().toVec2i();
				final Vector2i direction = new Vector2i(aPos).sub(bPos);
				final double dist = aPos.distance(bPos);
				
				final Vector2i aSize = room.metadata().tileSize();
				final Vector2i bSize = other.metadata().tileSize();
				final int aMax = Math.max(aSize.x, aSize.y);
				final int bMax = Math.max(bSize.x, bSize.y);
				
				final int target = (int)((aMax + bMax) * 0.5D) + MIN_SEP;
				if(dist < target)
					room.move(new Vector2i
							(
								(int)((direction.x / dist) * (target - dist)),
								(int)((direction.y / dist) * (target - dist))
							));
			}
		}
	}
}
