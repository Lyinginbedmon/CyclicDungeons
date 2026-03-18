package com.lying.grammar.content.trap;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.lying.data.CDTags;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.init.CDLoggers;
import com.lying.utility.BlockPredicate;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Describes a trap entry consisting of a small trap placed one or more times throughout a room after tile generation */
public abstract class AbstractPlacerTrap extends Trap
{
	protected RoomNumberProvider trapCounter = new RoomNumberProvider.Absolute(1);
	protected BlockPredicate viabilityCheck = BlockPredicate.Builder.create().build();
	protected int avoiderDistance = 3;
	protected int spacing = 1;
	
	protected AbstractPlacerTrap(Identifier idIn)
	{
		super(idIn);
	}
	
	protected AbstractPlacerTrap(Identifier idIn, RoomNumberProvider providerIn, BlockPredicate viabilityIn, int spacingIn, int avoidanceIn)
	{
		super(idIn);
		trapCounter = providerIn;
		viabilityCheck = viabilityIn;
		spacing = spacingIn;
		avoiderDistance = avoidanceIn;
	}
	
	public JsonObject asJsonObject()
	{
		JsonObject obj = super.asJsonObject();
		obj.add("Predicate", viabilityCheck.toJson(JsonOps.INSTANCE));
		obj.add("Count", trapCounter.toJson());
		obj.addProperty("Spacing", spacing);
		obj.addProperty("Avoidance", avoiderDistance);
		return obj;
	}
	
	protected Trap fromJson(JsonOps ops, JsonObject obj)
	{
		viabilityCheck = BlockPredicate.fromJson(ops, obj.getAsJsonObject("Predicate"));
		trapCounter = RoomNumberProvider.get(obj.get("Count"));
		spacing = obj.get("Spacing").getAsInt();
		avoiderDistance = obj.get("Avoidance").getAsInt();
		return this;
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		min = min.add(1, 0, 1);
		int floorY = min.getY() + Tile.TILE_SIZE;
		
		// Find all doors
		List<BlockPos> avoiders = Lists.newArrayList();
		BlockPos.Mutable.iterate(min.withY(floorY), max.withY(floorY)).forEach(p -> 
		{
			BlockPos next = p.toImmutable();
			BlockState state = world.getBlockState(next);
			if(state.isIn(BlockTags.DOORS) || state.isIn(CDTags.PLACER_AVOID))
				avoiders.add(next);
		});
		
		// Find all viable places for trap
		final Predicate<BlockPos> avoidFunc = minimumAvoiderDistance() == 0 ? Predicates.alwaysTrue() : p -> avoiders.stream().noneMatch(a -> a.getManhattanDistance(p) >= minimumAvoiderDistance());
		List<BlockPos> viablePoints = Lists.newArrayList();
		BlockPos.Mutable.iterate(min.withY(floorY), max.withY(floorY)).forEach(p -> 
		{
			BlockPos next = p.toImmutable();
			if(avoidFunc.test(next) && isPosViableForTrap(next, world))
				viablePoints.add(next);
		});
		if(viablePoints.isEmpty())
			CDLoggers.WORLDGEN.warn("Failed to find any place for trap");
		
		// FIXME Ensure unlimited placement when count for room is < 0
		
		Random rand = world.getRandom();
		if(trapCounter.isUnlimited())
			placeAllTraps(viablePoints, world, rand);
		else
		{
			// Select N places for traps
			List<BlockPos> traps = Lists.newArrayList();
			int i = trapCounter.getCount(rand, meta.tileSize());
			while(!viablePoints.isEmpty() && i-- > 0)
				traps.add(viablePoints.remove(rand.nextInt(viablePoints.size())));
			
			// Place traps
			placeAllTraps(traps, world, rand);
		}
	}
	
	protected final void placeAllTraps(List<BlockPos> positions, ServerWorld world, Random rand)
	{
		List<BlockPos> traps = Lists.newArrayList(positions);
		List<BlockPos> placedTraps = Lists.newArrayList();
		final Predicate<BlockPos> rectifier = p -> 
			!isPosViableForTrap(p, world) || 
			(minimumSpacing() > 0 && placedTraps.stream().anyMatch(p2 -> p.getManhattanDistance(p2) < minimumSpacing()));
		while(!traps.isEmpty())
		{
			BlockPos place = traps.remove(rand.nextInt(traps.size())); 
			placeTrap(place, world, rand);
			placedTraps.add(place);
			
			// Traps may be chosen to be placed in areas that invalidate other traps, so re-evaluate the set each time one is placed
			traps.removeIf(rectifier);
		}
	}
	
	/** Returns how far from any avoided blocks (such as doors) the traps should be placed */
	protected final int minimumAvoiderDistance() { return avoiderDistance; }
	
	/** Returns how far apart traps should be placed */
	protected final int minimumSpacing() { return spacing; }
	
	/** Returns true if the position is valid for trap placement */
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world) { return viabilityCheck.applyTo(pos, world); }
	
	/** Generates the trap in the world */
	protected abstract void placeTrap(BlockPos pos, ServerWorld world, Random rand);
}
