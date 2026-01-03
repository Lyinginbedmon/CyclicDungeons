package com.lying.worldgen.tile;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.tile.condition.Condition;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public abstract class RotationSupplier
{
	public static final Codec<RotationSupplier> CODEC = Codec.of(RotationSupplier::encode, RotationSupplier::decode);
	private static Map<Identifier,Supplier<RotationSupplier>> REGISTRY	= new HashMap<>();
	public static final Map<Direction, BlockRotation> faceToRotationMap = Map.of(
			Direction.NORTH, BlockRotation.NONE,
			Direction.EAST, BlockRotation.CLOCKWISE_90,
			Direction.SOUTH, BlockRotation.CLOCKWISE_180,
			Direction.WEST, BlockRotation.COUNTERCLOCKWISE_90
			);
	
	public static final Supplier<RotationSupplier> NONE		= register("none", id -> new RotationSupplier(id)
	{
		@NotNull
		public BlockRotation assignRotation(BlockPos pos, BlueprintTileGrid grid, Function<BlockPos,Optional<Tile>> func, Random rand) { return BlockRotation.NONE; }
	});
	public static final Supplier<RotationSupplier> RANDOM	= register("random", id -> new RotationSupplier(id)
	{
		@NotNull
		public BlockRotation assignRotation(BlockPos pos, BlueprintTileGrid grid, Function<BlockPos,Optional<Tile>> func, Random rand) { return BlockRotation.values()[rand.nextInt(BlockRotation.values().length)]; }
	});
	public static final Supplier<RotationSupplier> FACE_ADJACENT	= register("face_adjacent", FaceAdjacent::new);
	public static final Supplier<RotationSupplier> AGAINST_BOUNDARY	= register("face_boundary", FaceBoundary::new);
	
	private static Supplier<RotationSupplier> register(String name, Function<Identifier,RotationSupplier> factory)
	{
		return register(prefix(name), factory);
	}
	
	public static Supplier<RotationSupplier> register(Identifier id, Function<Identifier,RotationSupplier> factory)
	{
		Supplier<RotationSupplier> supplier = () -> factory.apply(id);
		REGISTRY.put(id, supplier);
		return supplier;
	}
	
	public static RotationSupplier get(Identifier id)
	{
		return REGISTRY.getOrDefault(id, NONE).get();
	}
	
	private final Identifier id;
	
	protected RotationSupplier(Identifier idIn)
	{
		id = idIn;
	}
	
	public final Identifier registryName() { return id; }
	
	@NotNull
	public abstract BlockRotation assignRotation(BlockPos pos, BlueprintTileGrid grid, Function<BlockPos,Optional<Tile>> func, Random rand);
	
	public JsonElement toJson(JsonOps ops)
	{
		// RotationSuppliers are stored as just their registry ID unless they necessitate more information
		return Identifier.CODEC.encodeStart(ops, id).getOrThrow();
	}
	
	/** Returns a JsonObject with the registry ID of this condition under "id" */
	protected final JsonObject asJsonObject(JsonOps ops)
	{
		JsonObject obj = new JsonObject();
		obj.add("id", ops.createString(id.toString()));
		return obj;
	}
	
	public RotationSupplier fromJson(JsonObject json, JsonOps ops) { return this; }
	
	@Nullable
	public static RotationSupplier fromJson(JsonElement json, JsonOps ops)
	{
		JsonObject obj;
		if(json.isJsonPrimitive())
			return RotationSupplier.get(Identifier.CODEC.parse(ops, json).getOrThrow());
		else if(json.isJsonObject() && (obj = json.getAsJsonObject()).has("id"))
		{
			RotationSupplier condition = RotationSupplier.get(Identifier.CODEC.parse(ops, obj.get("id")).getOrThrow());
			return obj.size() > 1 ? condition.fromJson(obj, ops) : condition;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final RotationSupplier func, final DynamicOps<T> ops, final T prefix)
	{
		return ops == JsonOps.INSTANCE ? (DataResult<T>)DataResult.success(func.toJson((JsonOps)ops)) : DataResult.error(() -> "Storing rotation supplier as NBT is not supported");
	}
	
	private static <T> DataResult<Pair<RotationSupplier, T>> decode(final DynamicOps<T> ops, final T input)
	{
		return ops == JsonOps.INSTANCE ? DataResult.success(Pair.of(fromJson((JsonElement)input, (JsonOps)ops), input)) : DataResult.error(() -> "Loading rotation supplier from NBT is not supported");
	}
	
	public static class FaceAdjacent extends RotationSupplier
	{
		private Condition predicate = CDTileConditions.NEVER.get();
		private RotationSupplier fallback = RotationSupplier.NONE.get();
		
		public FaceAdjacent(Identifier idIn)
		{
			super(idIn);
		}
		
		public static FaceAdjacent of(Condition predicateIn)
		{
			FaceAdjacent supplier = (FaceAdjacent)RotationSupplier.FACE_ADJACENT.get();
			supplier.predicate = predicateIn;
			return supplier;
		}
		
		public static FaceAdjacent of(Condition predicateIn, RotationSupplier fallbackIn)
		{
			FaceAdjacent supplier = of(predicateIn);
			supplier.fallback = fallbackIn;
			return supplier;
		}
		
		@NotNull
		public BlockRotation assignRotation(BlockPos pos, BlueprintTileGrid grid, Function<BlockPos, Optional<Tile>> func, Random rand)
		{
			for(Entry<Direction, BlockRotation> entry : faceToRotationMap.entrySet())
			{
				BlockPos offset = pos.offset(entry.getKey());
				Optional<Tile> neighbour = func.apply(offset);
				if(neighbour.isPresent() && predicate.test(neighbour.get(), offset, grid))
					return entry.getValue();
			}
			
			return fallback.assignRotation(pos, grid, func, rand);
		}
		
		public JsonElement toJson(JsonOps ops)
		{
			JsonObject obj = asJsonObject(ops);
			obj.add("condition", predicate.toJson(ops));
			obj.add("fallback", fallback.toJson(ops));
			return obj;
		}
		
		public RotationSupplier fromJson(JsonObject obj, JsonOps ops)
		{
			predicate = Condition.fromJson(obj.get("condition"), ops);
			fallback = RotationSupplier.fromJson(obj.get("fallback"), ops);
			return this;
		}
	}
	
	public static class FaceBoundary extends RotationSupplier
	{
		private RotationSupplier fallback = RotationSupplier.NONE.get();
		
		public FaceBoundary(Identifier idIn)
		{
			super(idIn);
		}
		
		public static FaceBoundary of(RotationSupplier fallbackIn)
		{
			FaceBoundary supplier = (FaceBoundary)RotationSupplier.AGAINST_BOUNDARY.get();
			supplier.fallback = fallbackIn;
			return supplier;
		}
		
		@NotNull
		public BlockRotation assignRotation(BlockPos pos, BlueprintTileGrid grid, Function<BlockPos, Optional<Tile>> func, Random rand)
		{
			for(Entry<Direction, BlockRotation> entry : faceToRotationMap.entrySet())
			{
				if(grid.isBoundary(pos, entry.getKey()))
					return entry.getValue();
			}
			
			return fallback.assignRotation(pos, grid, func, rand);
		}
		
		public JsonElement toJson(JsonOps ops)
		{
			JsonObject obj = asJsonObject(ops);
			obj.add("fallback", fallback.toJson(ops));
			return obj;
		}
		
		public RotationSupplier fromJson(JsonObject obj, JsonOps ops)
		{
			fallback = RotationSupplier.fromJson(obj.get("fallback"), ops);
			return this;
		}
	}
}