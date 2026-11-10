package com.lying.grammar.content.generation;

import static com.lying.worldgen.TileGenerator.setTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector2i;

import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.Content;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.GridVolume;
import com.lying.init.CDTileSets;
import com.lying.init.CDTiles;
import com.lying.reference.Reference;
import com.lying.worldgen.TileGenerator;
import com.lying.worldgen.TileGenerator.OptionLog;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tileset.TileSet;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class SimpleJumpingContent extends Content
{
	public static final Identifier ID	= Reference.ModInfo.prefix("simple_jumping");
	private Identifier floorTile = DefaultTiles.ID_PRISTINE_FLOOR;
	private Identifier fillTile = CDTiles.ID_STONE;
	private Optional<Identifier> treasureTile = Optional.empty();
	private Optional<Identifier> tileSet = Optional.empty();
	private Vector2i separations = new Vector2i(1,1);
	
	public SimpleJumpingContent(Identifier idIn)
	{
		super(idIn);
	}
	
	protected SimpleJumpingContent(Identifier idIn, Identifier fillTileID)
	{
		super(idIn);
		fillTile = fillTileID;
	}
	
	public static SimpleJumpingContent of(Identifier tileId)
	{
		return new SimpleJumpingContent(ID, tileId);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		obj.addProperty("Hazard", fillTile.toString());
		obj.addProperty("MinimumXSpacing", separations.x);
		obj.addProperty("MinimumZSpacing", separations.y);
		tileSet.ifPresent(id -> obj.addProperty("Platforms", id.toString()));
		treasureTile.ifPresent(id -> obj.addProperty("Treasure", id.toString()));
		return obj;
	}
	
	public Content fromJson(JsonOps ops, JsonObject obj)
	{
		super.fromJson(ops, obj);
		fillTile = Identifier.of(obj.get("Hazard").getAsString());
		separations = new Vector2i(
				Math.abs(obj.get("MinimumXSpacing").getAsInt()), 
				Math.abs(obj.get("MinimumZSpacing").getAsInt()));
		if(obj.has("Platforms"))
			tileSet = Optional.of(Identifier.of(obj.get("Platforms").getAsString()));
		if(obj.has("Treasure"))
			treasureTile = Optional.of(Identifier.of(obj.get("Treasure").getAsString()));
		return this;
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme)
	{
		for(Identifier id : List.of(fillTile,floorTile))
			if(CDTiles.instance().get(id).isEmpty())
			{
				CyclicDungeons.LOGGER.warn("? Content {} requires {} tile, but it is not present in datapack tile data", registryName().toString(), id.toString());
				return false;
			}
		
		return true;
	}
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, GridVolume volume, ServerWorld world, Random rand)
	{
		final TileSet baseTiles = room.metadata().theme().getTileSet(room.metadata().type());
		final TileSet tileSet = this.tileSet.isEmpty() ? baseTiles : CDTileSets.instance().get(this.tileSet.get()).orElse(baseTiles);
		final OptionLog optionManager = new OptionLog(tileMap, volume, tileSet.contents());
		
		// Place initial solid flooring underneath doorway exclusions
		List<BlockPos> platforms = new ArrayList<>();
		tileMap.getMatchingTilesWithin(volume, (p,t) -> t.registryName().equals(CDTiles.ID_PASSAGE_FLAG)).stream()
			.map(BlockPos::down)
			.filter(volume::contains)
			.forEach(p -> 
			{
				TileGenerator.processTile(p, tileMap, volume, tileSet, optionManager, CDTiles.STONE, rand);
				platforms.add(p);
			});
		
		// Randomly place solid flooring at distance from other solid flooring
		final int minSepX = separations.x;
		final int minSepY = separations.y;
		
		// TODO Implement platform height offsets
		
		List<BlockPos> floor = volume.getBoundaries(List.of(Direction.DOWN));
		List<BlockPos> options = new ArrayList<>();
		for(BlockPos p : floor)
			if(!volume.isSet(p) && !isPlatformWithin(p, platforms, minSepX, minSepY))
				options.add(p);
		
		while(!options.isEmpty())
		{
			// List of options within 1 step of the minimums from any existing platform
			// These are the positions we select from to ensure the intervening gap(s) are actually jumpable
			List<BlockPos> candidates = options.stream().filter(p -> isPlatformWithin(p, platforms, minSepX + 1, minSepY + 1)).toList();
			if(candidates.isEmpty())
				break;
			
			BlockPos pos = 
					candidates.size() == 1 ? 
						candidates.getFirst() : 
						candidates.get(rand.nextInt(candidates.size()));
			options.remove(pos);
			
			TileGenerator.processTile(pos, tileMap, volume, tileSet, optionManager, CDTiles.STONE, rand);
			platforms.add(pos);
			
			options.removeIf(p -> isPlatformWithin(p, platforms, minSepX, minSepY));
		}
		
		// Fill in remaining floor space with filler tile
		final Tile filler = CDTiles.instance().get(fillTile).get();
		for(BlockPos p : floor)
			if(!volume.isSet(p))
				setTile(p, filler, tileMap, volume);
		
		// Place treasure at farthest platform if this room is a dead end
		if(!room.hasChildren())
			treasureTile.ifPresent(id -> 
			{
				final BlockPos entry = tileMap.getMatchingTilesWithin(volume, (p,t) -> t.is(CDTiles.PASSAGE_FLAG.get())).getFirst();
				platforms.stream()
						.filter(p -> !tileMap.isBoundary(p, Direction.UP))
						.sorted((a,b) -> 
						{
							double distA = a.getSquaredDistance(entry);
							double distB = b.getSquaredDistance(entry);
							return distA > distB ? -1 : distA < distB ? 1 : 0;
						})
						.findFirst()
						.ifPresent(p -> setTile(p.up(), CDTiles.instance().getElse(id, CDTiles.BLANK), tileMap, volume));
			});
	}
	
	protected static boolean isPlatformWithin(BlockPos pos, List<BlockPos> platforms, int minSepX, int minSepZ)
	{
		for(BlockPos platform : platforms)
			if(
				(minSepX == 0 || Math.abs(platform.getX() - pos.getX()) <= minSepX) &&
				(minSepZ == 0 || Math.abs(platform.getZ() - pos.getZ()) <= minSepZ)
			)
				return true;
		return false;
	}
	
	public List<BlockPos> apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand) { return List.of(); }
}
