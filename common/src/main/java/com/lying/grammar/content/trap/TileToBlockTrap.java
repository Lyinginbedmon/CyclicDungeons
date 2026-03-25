package com.lying.grammar.content.trap;

import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTiles;
import com.lying.reference.Reference;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.lying.utility.BlockPredicate.SubPredicate;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TileToBlockTrap extends LogicControlledTrap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("tile_to_block");
	protected Identifier tileID = DefaultTiles.ID_HATCH;
	protected RoomNumberProvider tileCount = new RoomNumberProvider.SizeRatio(1, 1, 0.5);
	protected Identifier structureKey = Reference.ModInfo.prefix("trap/pressure_plate");
	protected BlockPredicate viabilityCheck = BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
			.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())).build();
	protected RoomNumberProvider structureCount = new RoomNumberProvider.RandBetween(1, 5, 2);
	protected BlockPos placementOffset = BlockPos.ORIGIN;
	
	protected TileTrap tileApplier;
	protected StructurePlacerTrap structureApplier;
	
	public TileToBlockTrap(Identifier nameIn)
	{
		super(nameIn);
	}
	
	protected TileToBlockTrap(Identifier nameIn, Identifier tileIn, RoomNumberProvider countIn, Identifier structureIn, RoomNumberProvider count2In, BlockPredicate predicate, BlockPos offset)
	{
		this(nameIn);
		tileID = tileIn;
		tileCount = countIn;
		structureKey = structureIn;
		structureCount = count2In;
		viabilityCheck = predicate;
		placementOffset = offset;
		
		updateSubTraps();
	}
	
	public static TileToBlockTrap of(Identifier tileIn, RoomNumberProvider tileCountIn, Identifier structureIn, RoomNumberProvider count2In, BlockPredicate predicate, BlockPos offset)
	{
		return new TileToBlockTrap(ID, tileIn, tileCountIn, structureIn, count2In, predicate, offset);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		super.toJson(obj, ops);
		obj.addProperty("Tile", tileID.toString());
		obj.add("TileCount", tileCount.toJson());
		
		obj.addProperty("Structure", structureKey.toString());
		obj.add("StructureCount", structureCount.toJson());
		obj.add("Predicate", viabilityCheck.toJson(ops));
		
		JsonArray off = new JsonArray();
		off.add(placementOffset.getX());
		off.add(placementOffset.getY());
		off.add(placementOffset.getZ());
		obj.add("Offset", off);
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonObject obj)
	{
		super.fromJson(ops, obj);
		tileID = Identifier.of(obj.get("Tile").getAsString());
		tileCount = RoomNumberProvider.get(obj.get("TileCount"));
		
		structureKey = Identifier.of(obj.get("Structure").getAsString());
		viabilityCheck = BlockPredicate.fromJson(ops, obj.getAsJsonObject("Predicate"));
		structureCount = RoomNumberProvider.get(obj.get("StructureCount"));
		JsonArray off = obj.getAsJsonArray("Offset");
		placementOffset = new BlockPos(off.get(0).getAsInt(), off.get(1).getAsInt(), off.get(2).getAsInt());
		
		updateSubTraps();
		return this;
	}
	
	protected void updateSubTraps()
	{
		tileApplier = TileTrap.of(tileID, tileCount, true);
		structureApplier = StructurePlacerTrap.of(structureKey, placementOffset, 0, 0, structureCount, viabilityCheck);
	}
	
	protected Optional<Tile> getTile() { return CDTiles.instance().get(tileID); }
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return tileApplier.isApplicableTo(room, meta, theme) && structureApplier.isApplicableTo(room, meta, theme); }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		tileApplier.prepare(room, tileMap, world);
	}
	
	protected void installSensors(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		structureApplier.apply(min, max, world, meta);
	}
}
