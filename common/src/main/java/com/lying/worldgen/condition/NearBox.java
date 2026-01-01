package com.lying.worldgen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class NearBox extends Condition
{
	protected Box bounds = Box.enclosing(BlockPos.ORIGIN, BlockPos.ORIGIN);
	protected Condition child = CDTileConditions.NEVER.get();
	
	public NearBox(Identifier idIn)
	{
		super(idIn);
	}
	
	public static NearBox of(Box boxIn, Condition childIn)
	{
		NearBox condition = (NearBox)CDTileConditions.NEAR_BOX.get();
		condition.bounds = boxIn;
		condition.child = childIn;
		return condition;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		final Box box = bounds.offset(pos);
		return !set.getMatchingTiles((p2,t2) -> 
			box.contains(new Vec3d(p2.getX() + 0.5D, p2.getY() + 0.5D, p2.getZ() + 0.5D)) && 
			child.test(t2, p2, set)).isEmpty();
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.add("min", BlockPos.CODEC.encodeStart(ops, BlockPos.ofFloored(bounds.minX, bounds.minY, bounds.minZ)).getOrThrow());
		obj.add("max", BlockPos.CODEC.encodeStart(ops, BlockPos.ofFloored(bounds.maxX, bounds.maxY, bounds.maxZ)).getOrThrow());
		obj.add("condition", child.toJson(ops));
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		bounds = Box.enclosing(BlockPos.CODEC.parse(ops, obj.get("min")).getOrThrow(), BlockPos.CODEC.parse(ops, obj.get("max")).getOrThrow());
		child = Condition.fromJson(obj.get("child"), ops);
		return this;
	}
	
	public static class Inverse extends NearBox
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(Box boxIn, Condition childIn)
		{
			Inverse condition = (Inverse)CDTileConditions.AVOID_BOX.get();
			condition.bounds = boxIn;
			condition.child = childIn;
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			final Box box = bounds.offset(pos);
			return set.getMatchingTiles((p2,t2) -> 
				box.contains(new Vec3d(p2.getX() + 0.5D, p2.getY() + 0.5D, p2.getZ() + 0.5D)) && 
				child.test(t2, p2, set)).isEmpty();
		}
	}
}