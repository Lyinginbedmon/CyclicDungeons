package com.lying.grammar.content;

import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;

public interface RoomNumberProvider
{
	public default int getCount(Random random, Vector2i roomSize)
	{
		return Math.max(0, apply(random, roomSize));
	}
	
	public int apply(Random random, Vector2i roomSize);
	
	public RoomNumberProviderType type();
	
	public JsonElement toJson();
	
	public default JsonObject toObj()
	{
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type().asString());
		return obj;
	}
	
	public RoomNumberProvider fromJson(JsonElement ele);
	
	public static RoomNumberProvider get(JsonElement ele)
	{
		RoomNumberProviderType type = RoomNumberProviderType.fromString(ele.isJsonPrimitive() ? ele.getAsString() : ele.getAsJsonObject().get("type").getAsString());
		return ele.isJsonObject() ? type.archetype().fromJson(ele) : type.archetype();
	}
	
	public static enum RoomNumberProviderType implements StringIdentifiable
	{
		ABSOLUTE(() -> new Absolute(1)),
		RAND_BETWEEN(() -> new RandBetween(1,2,0)),
		SIZE_RATIO(() -> new SizeRatio(1, 1, 1/8));
		
		private final Supplier<RoomNumberProvider> supplier;
		
		private RoomNumberProviderType(Supplier<RoomNumberProvider> supplierIn)
		{
			supplier = supplierIn;
		}
		
		public RoomNumberProvider archetype() { return supplier.get(); }
		
		public String asString() { return name().toLowerCase(); }
		
		public static RoomNumberProviderType fromString(String name)
		{
			for(RoomNumberProviderType type : values())
				if(type.asString().equalsIgnoreCase(name))
					return type;
			return ABSOLUTE;
		}
	}
	
	public static record Absolute(int val) implements RoomNumberProvider
	{
		public RoomNumberProviderType type() { return RoomNumberProviderType.ABSOLUTE; }
		
		public int apply(Random random, Vector2i roomSize) { return val; }
		
		public JsonElement toJson()
		{
			JsonObject obj = toObj();
			obj.addProperty("value", val);
			return obj;
		}
		
		public RoomNumberProvider fromJson(JsonElement ele)
		{
			return new Absolute(ele.getAsJsonObject().get("value").getAsInt());
		}
	}
	
	public static record RandBetween(int min, int max, int offset) implements RoomNumberProvider
	{
		public RoomNumberProviderType type() { return RoomNumberProviderType.RAND_BETWEEN; }
		
		public int apply(Random random, Vector2i roomSize) { return offset + random.nextBetween(min, max); }
		
		public JsonElement toJson()
		{
			JsonObject obj = toObj();
			obj.addProperty("min", min);
			obj.addProperty("max", max);
			if(offset != 0)
				obj.addProperty("offset", offset);
			return obj;
		}
		
		public RoomNumberProvider fromJson(JsonElement ele)
		{
			JsonObject obj = ele.getAsJsonObject();
			return new RandBetween(
					obj.get("min").getAsInt(), 
					obj.get("max").getAsInt(), 
					obj.has("offset") ? obj.get("offset").getAsInt() : 0);
		}
	}
	
	public static record SizeRatio(double widthScalar, double lengthScalar, double scalar) implements RoomNumberProvider
	{
		public RoomNumberProviderType type() { return RoomNumberProviderType.SIZE_RATIO; }
		
		public int apply(Random random, Vector2i roomSize)
		{
			return (int)(
					(widthScalar == 0 ? 1 : (roomSize.x * widthScalar)) *
					(lengthScalar == 0 ? 1 : (roomSize.y * lengthScalar)) *
					(scalar == 0 ? 1 : scalar)
					);
		}
		
		public JsonElement toJson()
		{
			JsonObject obj = toObj();
			if(widthScalar != 0)
				obj.addProperty("width", widthScalar);
			if(lengthScalar != 0)
				obj.addProperty("length", lengthScalar);
			if(scalar != 0)
				obj.addProperty("scalar", scalar);
			return obj;
		}
		
		public RoomNumberProvider fromJson(JsonElement ele)
		{
			JsonObject obj = ele.getAsJsonObject();
			return new RandBetween(
					obj.has("width") ? obj.get("width").getAsInt() : 0, 
					obj.has("length") ? obj.get("length").getAsInt() : 0, 
					obj.has("scalar") ? obj.get("scalar").getAsInt() : 0);
		}
		
	}
}
