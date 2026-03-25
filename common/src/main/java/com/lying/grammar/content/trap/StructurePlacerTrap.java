package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.init.CDLoggers;
import com.lying.reference.Reference;
import com.lying.utility.BlockPredicate;
import com.lying.utility.DebugLogger;
import com.mojang.serialization.JsonOps;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class StructurePlacerTrap extends AbstractPlacerTrap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("structure_placer");
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	private RegistryKey<StructurePool> structureKey = StructurePools.ofVanilla("placer_trap");
	private BlockPos offset = BlockPos.ORIGIN;
	
	public StructurePlacerTrap(Identifier idIn)
	{
		super(idIn);
	}
	
	protected StructurePlacerTrap(Identifier idIn, RoomNumberProvider countFunc, BlockPredicate viability, int spacing, int avoidance, Identifier keyIn, BlockPos offsetIn)
	{
		super(idIn, countFunc, viability, spacing, avoidance);
		structureKey = StructurePools.of(keyIn);
		offset = offsetIn;
	}
	
	public static StructurePlacerTrap of(Identifier keyIn, BlockPos offsetIn, int spacing, int avoidance, RoomNumberProvider countFunc, BlockPredicate viability)
	{
		return new StructurePlacerTrap(ID, countFunc, viability, spacing, avoidance, keyIn, offsetIn);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		super.toJson(obj, ops);
		obj.addProperty("Structure", structureKey.getValue().toString());
		
		JsonArray off = new JsonArray();
		off.add(offset.getX());
		off.add(offset.getY());
		off.add(offset.getZ());
		obj.add("Offset", off);
		
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonObject obj)
	{
		super.fromJson(ops, obj);
		structureKey = StructurePools.of(Identifier.of(obj.get("Structure").getAsString()));
		
		JsonArray off = obj.getAsJsonArray("Offset");
		offset = new BlockPos(off.get(0).getAsInt(), off.get(1).getAsInt(), off.get(2).getAsInt());
		
		return this;
	}
	
	protected void placeTrap(BlockPos pos, ServerWorld world, Random rand)
	{
		DynamicRegistryManager manager = world.getRegistryManager();
		Registry<StructurePool> registry = manager.getOrThrow(RegistryKeys.TEMPLATE_POOL);
		StructurePoolAliasLookup alias = StructurePoolAliasLookup.create(List.of(), pos, world.getSeed());
		Optional<StructurePool> poolOpt = Optional.of(structureKey).flatMap(key -> registry.getOptionalValue(alias.lookup(key)));
		if(poolOpt.isEmpty())
		{
			LOGGER.warn("Blank structure pool: {} for placer structure", structureKey.getValue().toString());
			return;
		}
		
		placeStructure(pos, poolOpt.get(), offset, world, rand);
	}
	
	protected static void placeStructure(BlockPos pos, StructurePool pool, BlockPos offset, ServerWorld world, Random rand)
	{
		StructureTemplateManager structureManager = world.getStructureTemplateManager();
		BlockPos place = pos.add(offset);
		StructurePoolElement element = pool.getRandomElement(rand);
		PoolStructurePiece piece = new PoolStructurePiece(
				structureManager,
				element,
				place,
				element.getGroundLevelDelta(),
				BlockRotation.NONE,
				element.getBoundingBox(structureManager, place, BlockRotation.NONE),
				StructureLiquidSettings.IGNORE_WATERLOGGING
				);
		piece.generate(
				world, 
				world.getStructureAccessor(), 
				world.getChunkManager().getChunkGenerator(), 
				rand, 
				piece.getBoundingBox(), 
				place, 
				false);
	}
}
