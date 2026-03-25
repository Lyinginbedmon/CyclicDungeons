package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.reference.Reference;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.mojang.serialization.JsonOps;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class SatelliteStructurePlacerTrap extends StructurePlacerTrap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("satellite_placer");
	protected BlockPredicate adjacentCheck = BlockPredicate.Builder.create().addFlag(BlockFlags.PLAYER_ACCESSIBLE).build();
	private RegistryKey<StructurePool> adjacentKey = StructurePools.ofVanilla("placer_trap");
	private BlockPos adjacentOffset = BlockPos.ORIGIN;
	
	public SatelliteStructurePlacerTrap(Identifier idIn)
	{
		super(idIn);
	}
	
	protected SatelliteStructurePlacerTrap(Identifier idIn, 
			RoomNumberProvider countFunc, BlockPredicate viability, int spacing, int avoidance, 
			Identifier keyIn, BlockPos offsetIn,
			BlockPredicate viability2In, Identifier key2In, BlockPos offset2In)
	{
		super(idIn, countFunc, viability, spacing, avoidance, keyIn, offsetIn);
		adjacentCheck = viability2In;
		adjacentKey = StructurePools.of(key2In);
		adjacentOffset = offset2In;
	}
	
	public static SatelliteStructurePlacerTrap of(
			int spacing, int avoidance, RoomNumberProvider countFunc, 
			BlockPredicate coreViability, Identifier coreKey, BlockPos coreOffset,
			BlockPredicate adjacentViability, Identifier adjacentKey, BlockPos adjacentOffset
			)
	{
		return new SatelliteStructurePlacerTrap(ID, countFunc, coreViability, spacing, avoidance, coreKey, coreOffset, adjacentViability, adjacentKey, adjacentOffset);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		super.toJson(obj, ops);
		obj.addProperty("Structure2", adjacentKey.getValue().toString());
		obj.add("Predicate2", adjacentCheck.toJson(ops));
		
		JsonArray off = new JsonArray();
		off.add(adjacentOffset.getX());
		off.add(adjacentOffset.getY());
		off.add(adjacentOffset.getZ());
		obj.add("Offset2", off);
		
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonObject obj)
	{
		super.fromJson(ops, obj);
		adjacentKey = StructurePools.of(Identifier.of(obj.get("Structure2").getAsString()));
		adjacentCheck = BlockPredicate.fromJson(ops, obj.getAsJsonObject("Predicate2"));
		
		JsonArray off = obj.getAsJsonArray("Offset2");
		adjacentOffset = new BlockPos(off.get(0).getAsInt(), off.get(1).getAsInt(), off.get(2).getAsInt());
		
		return this;
	}
	
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world)
	{
		return 
				super.isPosViableForTrap(pos, world) && 
				Direction.Type.HORIZONTAL.stream().map(pos::offset).anyMatch(p -> 
					adjacentCheck.applyTo(p, world));
	}
	
	protected void placeTrap(BlockPos pos, ServerWorld world, Random rand)
	{
		DynamicRegistryManager manager = world.getRegistryManager();
		Registry<StructurePool> registry = manager.getOrThrow(RegistryKeys.TEMPLATE_POOL);
		StructurePoolAliasLookup alias = StructurePoolAliasLookup.create(List.of(), pos, world.getSeed());
		Optional<StructurePool> poolOpt = Optional.of(adjacentKey).flatMap(key -> registry.getOptionalValue(alias.lookup(key)));
		if(poolOpt.isEmpty())
		{
			LOGGER.warn("Blank structure pool: {} for satellite placer structure", adjacentKey.getValue().toString());
			return;
		}
		
		super.placeTrap(pos, world, rand);
		
		Direction.Type.HORIZONTAL.stream()
			.map(pos::offset)
			.filter(p -> adjacentCheck.applyTo(p, world))
			.forEach(p -> StructurePlacerTrap.placeStructure(p, poolOpt.get(), adjacentOffset, world, rand));
	}
}
