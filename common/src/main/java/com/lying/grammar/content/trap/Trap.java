package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTrapTypes;
import com.lying.worldgen.theme.Theme;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;

public abstract class Trap
{
	public static final Codec<Trap> CODEC = Codec.of(Trap::encode, Trap::decode);
	
	private final Identifier registryName;
	protected boolean allowDeadEnds = true;
	
	protected Trap(Identifier nameIn)
	{
		registryName = nameIn;
	}
	
	public final Identifier registryName() { return registryName; }
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final Trap trap, final DynamicOps<T> ops, final T prefix)
	{
		if(ops != JsonOps.INSTANCE)
			return DataResult.error(() -> "Storing trap as NBT is not supported");
		
		return (DataResult<T>)DataResult.success(trap.toJson(JsonOps.INSTANCE));
	}
	
	private static <T> DataResult<Pair<Trap, T>> decode(final DynamicOps<T> ops, final T input)
	{
		if(ops != JsonOps.INSTANCE)
			return DataResult.error(() -> "Loading trap from NBT is not supported");
		
		JsonElement ele = (JsonElement)input;
		if(ele.isJsonPrimitive())
		{
			Optional<Trap> trap = CDTrapTypes.get(Identifier.of(ele.getAsString())); 
			return trap.isPresent() ? DataResult.success(Pair.of(trap.get(), input)) : DataResult.error(() -> "Error loading trap from data, type unrecognised");
		}
		else
		{
			JsonObject obj = ele.getAsJsonObject();
			Optional<Trap> trapOpt = CDTrapTypes.get(Identifier.of(obj.get("Type").getAsString()));
			if(trapOpt.isEmpty())
				return DataResult.error(() -> "Error loading trap from data, type unrecognised");
			
			Trap trap = trapOpt.get();
			obj.remove("Type");
			if(!obj.isEmpty())
				trap = trap.fromJson(JsonOps.INSTANCE, ele);
			return DataResult.success(Pair.of(trap, input));
				
		}
	}
	
	/** Returns true if this entry can be applied to the given room */
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return allowDeadEnds || room.hasChildren(); }
	
	/** Applied when the entry is selected, before the room goes through tile generation */
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world) { }
	
	/** Applied after tile generation */
	public abstract void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta);
	
	public JsonElement toJson(JsonOps ops){ return new JsonPrimitive(registryName().toString()); }
	
	/** Loads all configured values from the given element into this trap */
	protected Trap fromJson(JsonOps ops, JsonElement ele) { return this; }
	
	/** Returns a JSON object containing this trap's registry name */
	protected JsonObject asJsonObject()
	{
		JsonObject obj = new JsonObject();
		obj.addProperty("Type", registryName().toString());
		return obj;
	}
	
	/** Returns all matching tile entities within the specified bounds */
	public static <T extends BlockEntity> List<T> getTileEntitiesWithin(BlockPos min, BlockPos max, ServerWorld world, BlockEntityType<T> type)
	{
		List<T> tiles = Lists.newArrayList();
		for(int x=min.getX(); x<max.getX(); x++)
			for(int z=min.getZ(); z<max.getZ(); z++)
				for(int y=min.getY(); y<max.getY(); y++)
					world.getBlockEntity(new BlockPos(x, y, z), type).ifPresent(tiles::add);
		
		return tiles;
	}
	
	public static Optional<BlockPos> getCeilingAbove(BlockPos pos, ServerWorld world) { return getCeilingAbove(pos, world, 10); }
	
	// FIXME Ensure raytrace actually succeeds
	public static Optional<BlockPos> getCeilingAbove(BlockPos pos, ServerWorld world, int maxRange)
	{
		BlockPos top = pos.offset(Direction.UP, maxRange);
		BlockStateRaycastContext context = new BlockStateRaycastContext(
				new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5D),
				new Vec3d(top.getX(), top.getY(), top.getZ()).add(0.5D),
				s -> s.isAir());
		
		BlockHitResult trace = world.raycast(context);
		return trace.getType() != Type.BLOCK ? Optional.empty() : Optional.of(trace.getBlockPos());
	}
}
