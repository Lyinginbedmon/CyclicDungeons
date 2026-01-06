package com.lying.grammar;

import org.joml.Vector2i;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.random.Random;

public record SizeFunction(int minX, int maxX, int minY, int maxY)
{
	public static final Codec<SizeFunction> CODEC = Codec.of(SizeFunction::encode, SizeFunction::decode);
	
	public Vector2i apply(Random rand)
	{
		int x = minX == maxX ? minX : minX + rand.nextInt(maxX - minX);
		int y = minY == maxY ? minY : minY + rand.nextInt(maxY - minY);
		return new Vector2i(x, y);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final SizeFunction func, final DynamicOps<T> ops, final T prefix)
	{
		if(ops == JsonOps.INSTANCE)
		{
			JsonObject obj = new JsonObject();
			if(func.minX == func.maxX)
				obj.addProperty("x", func.minX);
			else
			{
				obj.addProperty("x_min", func.minX);
				obj.addProperty("x_max", func.maxX);
			}
			if(func.minY == func.maxY)
				obj.addProperty("y", func.minY);
			else
			{
				obj.addProperty("y_min", func.minY);
				obj.addProperty("y_max", func.maxY);
			}
			return (DataResult<T>)DataResult.success(obj);
		}
		else if(ops == NbtOps.INSTANCE)
		{
			NbtCompound nbt = new NbtCompound();
			if(func.minX == func.maxX)
				nbt.putInt("x", func.minX);
			else
			{
				nbt.putInt("x_min", func.minX);
				nbt.putInt("x_max", func.maxX);
			}
			if(func.minY == func.maxY)
				nbt.putInt("y", func.minY);
			else
			{
				nbt.putInt("y_min", func.minY);
				nbt.putInt("y_max", func.maxY);
			}
			return (DataResult<T>)DataResult.success(nbt);
		}
		
		return DataResult.error(() -> "Failed to store size function");
	}
	
	private static <T> DataResult<Pair<SizeFunction, T>> decode(final DynamicOps<T> ops, final T input)
	{
		int 
			minX = 0, 
			maxX = 0, 
			minY = 0, 
			maxY = 0;
		if(ops == JsonOps.INSTANCE)
		{
			JsonElement ele = (JsonElement)input;
			JsonObject obj = ele.getAsJsonObject();
			if(obj.has("x"))
				minX = maxX = obj.get("x").getAsInt();
			else
			{
				minX = obj.get("x_min").getAsInt();
				maxX = obj.get("x_max").getAsInt();
			}
			
			if(obj.has("y"))
				minY = maxY = obj.get("y").getAsInt();
			else
			{
				minY = obj.get("y_min").getAsInt();
				maxY = obj.get("y_max").getAsInt();
			}
		}
		else if(ops == NbtOps.INSTANCE)
		{
			NbtCompound nbt = (NbtCompound)input;
			if(nbt.contains("x"))
				minX = maxX = nbt.getInt("x");
			else
			{
				minX = nbt.getInt("x_min");
				maxX = nbt.getInt("x_max");
			}
			
			if(nbt.contains("y"))
				minY = maxY = nbt.getInt("y");
			else
			{
				minY = nbt.getInt("y_min");
				maxY = nbt.getInt("y_max");
			}
		}
		
		return DataResult.success(Pair.of(new SizeFunction(minX, maxX, minY, maxY), input));
	}
}