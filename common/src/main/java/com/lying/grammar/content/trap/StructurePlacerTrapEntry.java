package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import org.joml.Vector2i;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grammar.content.IContentEntry;
import com.lying.init.CDLoggers;
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

public class StructurePlacerTrapEntry extends AbstractPlacerTrapEntry
{
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	private RegistryKey<StructurePool> structureKey = StructurePools.ofVanilla("placer_trap");
	private BlockPos offset = BlockPos.ORIGIN;
	private int avoiderDistance = 3;
	private int spacing = 1;
	
	public StructurePlacerTrapEntry(Identifier idIn)
	{
		super(idIn);
	}
	
	public StructurePlacerTrapEntry(Identifier idIn, Identifier keyIn, BlockPos offsetIn)
	{
		this(idIn);
		structureKey = StructurePools.of(keyIn);
		offset = offsetIn;
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject();
		obj.addProperty("Structure", structureKey.getValue().toString());
		obj.addProperty("Spacing", spacing);
		obj.addProperty("Avoidance", avoiderDistance);
		
		JsonArray off = new JsonArray();
		off.add(offset.getX());
		off.add(offset.getY());
		off.add(offset.getZ());
		obj.add("Offset", off);
		
		return obj;
	}
	
	public IContentEntry fromJson(JsonOps ops, JsonElement ele)
	{
		JsonObject obj = ele.getAsJsonObject();
		structureKey = StructurePools.of(Identifier.of(obj.get("Structure").getAsString()));
		spacing = obj.get("Spacing").getAsInt();
		avoiderDistance = obj.get("Avoidance").getAsInt();
		
		JsonArray off = obj.getAsJsonArray("Offset");
		offset = new BlockPos(off.get(0).getAsInt(), off.get(1).getAsInt(), off.get(2).getAsInt());
		
		return this;
	}
	
	protected int minimumAvoiderDistance() { return avoiderDistance; }
	
	protected int minimumSpacing() { return spacing; }
	
	protected int getTrapCountForRoom(Random rand, Vector2i roomSize)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world)
	{
		// TODO Auto-generated method stub
		return false;
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
		
		StructureTemplateManager structureManager = world.getStructureTemplateManager();
		BlockPos place = pos.add(offset);
		StructurePoolElement element = poolOpt.get().getRandomElement(rand);
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
