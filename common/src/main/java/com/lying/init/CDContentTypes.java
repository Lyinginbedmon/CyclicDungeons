package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.lying.CyclicDungeons;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.Content;
import com.lying.grammar.content.IContentEntry;
import com.lying.grammar.content.generation.ModularContent;
import com.lying.grammar.content.generation.SatelliteStructurePlacerContent;
import com.lying.grammar.content.generation.SimpleJumpingContent;
import com.lying.grammar.content.generation.StructurePlacerContent;
import com.lying.grammar.content.generation.TileContent;
import com.lying.grammar.content.generation.TileSetContent;
import com.lying.grammar.content.generation.TileToBlockContent;
import com.lying.grid.BlueprintTileGrid;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tileset.DoorWaySet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class CDContentTypes
{
	private static final Map<Identifier, Supplier<Content>> TRAPS	= new HashMap<>();
	
	/*
	 * Chaser corridor trap - As corridor but traps fired regularly in overt sequence
	 * Warden trap - Spawners of thematic mobs triggered by sight sensor
	 * Corridor trap - Long room lined with darts/spikes and pressure plates
	 * Dart hail trap - Abundance of dart traps triggered by collision sensors on floor
	 * Spear corridor - Spike traps triggered in sequence by a clock
	 * Spear parkour - Spike traps on walls triggered in sequence by a clock, above pits
	 * Ceiling blade pendulums - Array of blade traps triggered in sequence by a clock
	 * Fusillade trap - Abundance of dart trips triggered in sequence by a clock
	 */
	
	// Configurable
	public static final Supplier<Content> STRUCTURE_PLACER		= register(StructurePlacerContent.ID, StructurePlacerContent::new);
	public static final Supplier<Content> ADJACENT_PLACER		= register(SatelliteStructurePlacerContent.ID, SatelliteStructurePlacerContent::new);
	public static final Supplier<Content> SIMPLE_JUMPER		= register(SimpleJumpingContent.ID, SimpleJumpingContent::new);
	public static final Supplier<Content> TILE_PREGEN			= register(TileContent.ID, TileContent::new);
	public static final Supplier<Content> TILE_SET_PREGEN		= register(TileSetContent.ID, TileSetContent::new);
	public static final Supplier<Content> TILE_TO_BLOCK		= register(TileToBlockContent.ID, TileToBlockContent::new);
	public static final Supplier<Content> MODULAR				= register(ModularContent.ID, ModularContent::new);
	
	public static Supplier<Content> register(Identifier name, Function<Identifier, Content> func)
	{
		Supplier<Content> supplier = () -> func.apply(name);
		TRAPS.put(name, supplier);
		return supplier;
	}
	
	public static Optional<Content> get(Identifier name)
	{
		return TRAPS.containsKey(name) ? Optional.of(TRAPS.get(name).get()) : Optional.empty();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised {} content types", TRAPS.size());
	}
	
	public static record ContentEntry(Identifier registryName, Content type) implements IContentEntry
	{
		public static final Codec<ContentEntry> CODEC	= Codec.of(ContentEntry::encode, ContentEntry::decode);
		
		public Text describe() { return Text.empty(); }
		
		@SuppressWarnings("unchecked")
		private static <T> DataResult<T> encode(final ContentEntry trap, final DynamicOps<T> ops, final T prefix)
		{
			if(ops != JsonOps.INSTANCE)
				return DataResult.error(() -> "Storing content entry as NBT is not supported");
			
			return (DataResult<T>)DataResult.success(trap.toJson(JsonOps.INSTANCE));
		}
		
		private static <T> DataResult<Pair<ContentEntry, T>> decode(final DynamicOps<T> ops, final T input)
		{
			if(ops != JsonOps.INSTANCE)
				return DataResult.error(() -> "Loading content entry from NBT is not supported");
			
			ContentEntry entry = fromJson(JsonOps.INSTANCE, (JsonObject)input);
			return entry == null ? DataResult.error(() -> "Error loading content entry from JSON") : DataResult.success(Pair.of(entry, input));
		}
		
		/** Returns true if this entry can be applied to the given room */
		public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return type.isApplicableTo(room, meta, theme); }
		
		/** Applied when the entry is selected, before the room goes through tile generation */
		public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world, Random rand) { type.prepare(room, tileMap, world, rand); }
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand) { type.apply(min, max, world, meta, rand); }
		
		public List<DoorWaySet> getExitDoorTiles(Random rand, int count)
		{
			return type.getExitDoorTiles(rand, count);
		}
		
		public DoorWaySet getEntryDoorTiles() { return type.getEntryDoorTiles(); }
		
		public JsonObject toJson(JsonOps ops)
		{
			JsonObject obj = new JsonObject();
			obj.add("Name", Identifier.CODEC.encodeStart(ops, registryName()).getOrThrow());
			obj.add("Type", Identifier.CODEC.encodeStart(ops, type.registryName()).getOrThrow());
			type.getConfig().ifPresent(c -> obj.add("Settings", c));
			return obj;
		}
		
		@Nullable
		public static ContentEntry fromJson(JsonOps ops, JsonObject obj)
		{
			Identifier name = Identifier.CODEC.parse(ops, obj.get("Name")).getOrThrow();
			Optional<Content> type = CDContentTypes.get(Identifier.CODEC.parse(ops, obj.get("Type")).getOrThrow());
			if(type.isEmpty())
				return null;
			
			Content trap = type.get();
			if(obj.has("Settings"))
				trap = trap.fromJson(ops, obj.getAsJsonObject("Settings"));
			return new ContentEntry(name, trap);
		}
	}
}
