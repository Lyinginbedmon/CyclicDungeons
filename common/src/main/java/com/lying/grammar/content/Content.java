package com.lying.grammar.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDContentTypes;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tileset.DoorWaySet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public abstract class Content
{
	public static final Codec<Content> CODEC = Identifier.CODEC.comapFlatMap(id -> 
	{
		Optional<Content> type = CDContentTypes.get(id);
		return type.isEmpty() ? DataResult.error(() -> "Content type unrecognised: "+id.toString()) : DataResult.success(type.get());
	}, Content::registryName);
	
	private final Identifier registryName;
	protected ContentViability prerequisites = ContentViability.Builder.create().build();
	protected Optional<DoorWaySet> entryDoors = Optional.empty();
	
	protected ExitStyle exitStyle = ExitStyle.UNIFIED;
	protected Optional<List<DoorWaySet>> exitDoors = Optional.empty();
	
	protected Content(Identifier nameIn)
	{
		registryName = nameIn;
	}
	
	public final Identifier registryName() { return registryName; }
	
	public final Content setConditions(ContentViability conditionsIn)
	{
		prerequisites = conditionsIn;
		return this;
	}
	
	/** Returns true if this entry can be applied to the given room */
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme)
	{
		return prerequisites.isViable(room, meta, theme);
	}
	
	/** Applied when the entry is selected, before the room goes through tile generation */
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world, Random rand) { }
	
	/** Applied after tile generation */
	public abstract void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand);
	
	public DoorWaySet getEntryDoorTiles() { return entryDoors.orElse(DoorWaySet.BLANK); }
	
	public List<DoorWaySet> getExitDoorTiles(Random rand, int count)
	{
		if(exitDoors.isEmpty() || exitDoors.get().isEmpty())
			return List.of(DoorWaySet.BLANK);
		
		List<DoorWaySet> set = exitDoors.get();
		if(set.size() == 1)
			return set;
		
		switch(exitStyle)
		{
			case ITERATE:
				if(count >= set.size())
					return set;
				else
					return set.subList(0, count);
			case RANDOM:
				List<DoorWaySet> sequence = new ArrayList<>();
				for(int i=0; i<count; i++)
					sequence.add(set.get(rand.nextInt(set.size())));
				return sequence;
			case UNIFIED:
			default:
				return List.of(set.get(rand.nextInt(set.size())));
		}
	}
	
	public Content setEntryDoorTiles(DoorWaySet set)
	{
		entryDoors = set == null ? Optional.empty() : Optional.of(set);
		return this;
	}
	
	public Content setExitDoorTiles(DoorWaySet... set)
	{
		if(set.length == 0)
			exitDoors = Optional.empty();
		else
		{
			List<DoorWaySet> list = new ArrayList<>();
			for(DoorWaySet doors : set)
				if(doors != null)
					list.add(doors);
			exitDoors = list.isEmpty() ? Optional.empty() : Optional.of(list);
		}
		return this;
	}
	
	public Content setDoorTiles(DoorWaySet entry, DoorWaySet exit)
	{
		setEntryDoorTiles(entry);
		setExitDoorTiles(exit);
		return this;
	}
	
	public final Optional<JsonObject> getConfig()
	{
		JsonObject obj = toJson(new JsonObject(), JsonOps.INSTANCE);
		
		JsonObject conditions = prerequisites.toJson();
		if(!conditions.isEmpty())
			obj.add("Prerequisites", conditions);
		if(entryDoors.isPresent())
			obj.add("DoorwaysIn", entryDoors.get().toJson());
		
		if(exitDoors.isPresent())
		{
			if(exitStyle != ExitStyle.UNIFIED)
				obj.addProperty("ExitStyle", exitStyle.asString());
			
			JsonArray set = new JsonArray();
			for(DoorWaySet entry : exitDoors.get())
				if(entry != null)
					set.add(entry.toJson());
			obj.add("DoorwaysOut", set);
		}
		return obj.isEmpty() ? Optional.empty() : Optional.of(obj);
	}
	
	/** Stores all configured values from this trap into the given JsonObject, specific to each descendant class of Content */
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		return obj;
	}
	
	/** Loads all configured values from the given element into this trap */
	public Content fromJson(JsonOps ops, JsonObject obj)
	{
		if(obj.has("Prerequisites"))
			prerequisites = ContentViability.fromJson(obj.get("Prerequisites").getAsJsonObject());
		if(obj.has("DoorwaysIn"))
			entryDoors = Optional.of(DoorWaySet.fromJson(obj.get("DoorwaysIn")));
		
		if(obj.has("DoorwaysOut"))
		{
			if(obj.has("ExitStyle"))
				exitStyle = ExitStyle.fromString(obj.get("ExitStyle").getAsString());
			
			JsonArray set = obj.getAsJsonArray("DoorwaysOut");
			List<DoorWaySet> list = new ArrayList<>();
			for(int i=0; i<set.size(); i++)
			{
				DoorWaySet entry = DoorWaySet.fromJson(set.get(i));
				if(entry != null)
					list.add(entry);
			}
			exitDoors = list.isEmpty() ? Optional.empty() : Optional.of(list);
		}
		return this;
	}
	
	private static enum ExitStyle implements StringIdentifiable
	{
		UNIFIED,
		RANDOM,
		ITERATE;
		
		public String asString() { return name().toLowerCase(); }
		
		public static ExitStyle fromString(String name)
		{
			for(ExitStyle style : values())
				if(style.asString().equalsIgnoreCase(name))
					return style;
			return UNIFIED;
		}
	}
}
