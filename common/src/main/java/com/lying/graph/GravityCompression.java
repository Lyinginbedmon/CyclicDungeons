package com.lying.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintRoom;
import com.lying.init.CDTerms;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

/** Verlet-based approach to dungeon compression, using each room's parent as its gravitational direction */
public class GravityCompression
{
	/** Minimum number of tiles between adjacent unconnected room grids */
	private static int MIN_SEP;
	
	public static void collapse(Blueprint chart)
	{
		scrunch(chart, 1000);
	}
	
	public static void scrunch(Blueprint chart)
	{
		scrunch(chart, -1);
	}
	
	public static void scrunch(Blueprint chart, final int cap)
	{
		MIN_SEP = 1 + chart.theme().verletCompressionSpacing();
		
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
		
		// Step 1: Attract all rooms to their parents, respecting boundaries their depth or lower
		int limit = cap;
		boolean changed = false;
		do
		{
			for(int i=1; i<=chart.maxDepth(); i++)
				for(BlueprintRoom room : byDepth.getOrDefault(i, List.of()))
					changed = verletObjects.get(room).applyTo(room, depthColliders.get(i)) || changed;
		}
		while(limit-- > 0 && changed);
		
		// Step 2: Generate passages inbetween rooms
		chart.clearPassageCache();
		chart.passages();
		
		// Step 3: Apply conventional compression to finalise
		if(chart.theme().shouldApplyGraphCompression())
			GraphCompression.collapse(chart);
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
			final Vector2i aSize = room.metadata().tileSize();
			final int lowestY = aSize.y / 2;
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
				
				final Vector2i bSize = other.metadata().tileSize();
				final Vector2i bPos = other.tilePosition().toVec2i();
				
				Vector2i aPos;
				if(colliding(aPos = room.tilePosition().toVec2i(), aSize, bPos, bSize))
				{
					final Vector2i direction = new Vector2i(aPos).sub(bPos);
					final Direction move = 
							direction.length() == 0 ? 
									Direction.SOUTH : 
									Direction.fromVector((int)direction.x, 0, (int)direction.y, Direction.NORTH);
					
					// How far we are from each-other on this axis
					int dist = 0;
					// How far we need to be from each-other on this axis
					int target = MIN_SEP;
					if(move.getAxis() == Axis.X)
					{
						dist = Math.abs(direction.x);
						if(move.getOffsetX() < 0)
							target += (bSize.x / 2) + aSize.x - (aSize.x / 2);
						else
							target += bSize.x - (bSize.x / 2) + aSize.x / 2;
					}
					else
					{
						dist = Math.abs(direction.y);
						if(move.getOffsetY() < 0)
							target += (bSize.y / 2) + aSize.y - (aSize.y / 2);
						else
							target += bSize.y - (bSize.y / 2) + aSize.y / 2;
					}
					
					room.move(
							new Vector2i(move.getOffsetX(), move.getOffsetZ())
							.mul(target - dist));
				}
			}
		}
		
		private static boolean colliding(Vector2i aPos, Vector2i aSize, Vector2i bPos, Vector2i bSize)
		{
			final Vector2i direction = new Vector2i(aPos).sub(bPos);
			if(direction.length() == 0)
				return true;
			
			final Direction move = Direction.fromVector((int)direction.x, 0, (int)direction.y, Direction.NORTH);
			// How far we are from each-other on this axis
			int dist = 0;
			// How far we need to be from each-other on this axis
			int target = MIN_SEP;
			if(move.getAxis() == Axis.X)
			{
				dist = Math.abs(direction.x);
				if(move.getOffsetX() < 0)
					target += (bSize.x / 2) + aSize.x - (aSize.x / 2);
				else
					target += bSize.x - (bSize.x / 2) + aSize.x / 2;
			}
			else
			{
				dist = Math.abs(direction.y);
				if(move.getOffsetY() < 0)
					target += (bSize.y / 2) + aSize.y - (aSize.y / 2);
				else
					target += bSize.y - (bSize.y / 2) + aSize.y / 2;
			}
			return dist < target;
		}
	}
}
