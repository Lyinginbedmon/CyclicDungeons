package com.lying.grammar.content;

import org.joml.Vector2i;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.worldgen.theme.Theme;

public class ContentViability
{
	protected final Vector2i exitCountRange;
	
	private ContentViability(Vector2i exitRange)
	{
		exitCountRange = exitRange;
	}
	
	public JsonObject toJson()
	{
		JsonObject obj = new JsonObject();
		if(exitCountRange.x != -1 || exitCountRange.y != -1)
		{
			JsonArray exits = new JsonArray();
			exits.add(exitCountRange.x);
			exits.add(exitCountRange.y);
			obj.add("Exits", exits);
		}
		return obj;
	}
	
	public static ContentViability fromJson(JsonObject obj)
	{
		Builder builder = Builder.create();
		if(obj.has("Exits"))
		{
			JsonArray exits = obj.get("Exits").getAsJsonArray();
			builder.setExitRange(exits.get(0).getAsInt(), exits.get(1).getAsInt());
		}
		
		return builder.build();
	}
	
	public boolean isViable(BlueprintRoom room, RoomMetadata meta, Theme theme)
	{
		int exits = room.childrenCount();
		if(
			exitCountRange.x >= 0 && exits < exitCountRange.x || 
			exitCountRange.y >= 0 && exits > exitCountRange.y)
			return false;
		
		return true;
	}
	
	public static class Builder
	{
		private Vector2i exitRange = new Vector2i(-1, -1);
		
		private Builder() { }
		
		public static Builder create() { return new Builder(); }
		
		public Builder setExitRange(int min, int max)
		{
			setMinimumExits(min);
			setMaximumExits(max);
			return this;
		}
		
		public Builder setMaximumExits(int val)
		{
			exitRange = new Vector2i(exitRange.x, val < 0 ? -1 : val);
			return this;
		}
		
		public Builder setMinimumExits(int val)
		{
			exitRange = new Vector2i(val < 0 ? -1 : val, exitRange.y);
			return this;
		}
		
		public ContentViability build()
		{
			return new ContentViability(exitRange);
		}
	}
}
