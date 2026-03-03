package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.init.CDTraps;
import com.lying.worldgen.theme.Theme;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;

public class TrapRoomContent extends RegistryRoomContent<TrapEntry>
{
	public static final Identifier ID	= prefix("trap");
	
	public TrapRoomContent()
	{
		super(ID);
	}
	
	public void buildRegistry(Theme theme)
	{
		theme.traps().forEach(trap -> register(trap.registryName(), trap));
	}
	
	public static abstract class TrapEntry implements IContentEntry
	{
		public static final Codec<TrapEntry> CODEC = Codec.of(TrapEntry::encode, TrapEntry::decode);
		
		private final Identifier id;
		
		protected TrapEntry(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
		
		@SuppressWarnings("unchecked")
		private static <T> DataResult<T> encode(final TrapEntry func, final DynamicOps<T> ops, final T prefix)
		{
			return ops == JsonOps.INSTANCE ? (DataResult<T>)DataResult.success(func.toJson(JsonOps.INSTANCE)) : DataResult.error(() -> "Storing trap entry as NBT is not supported");
		}
		
		private static <T> DataResult<Pair<TrapEntry, T>> decode(final DynamicOps<T> ops, final T input)
		{
			if(ops != JsonOps.INSTANCE)
				return DataResult.error(() -> "Loading trap entry from NBT is not supported");
			
			Optional<TrapEntry> entry = createFromJson(JsonOps.INSTANCE, (JsonElement)input);
			return entry.isPresent() ? DataResult.success(Pair.of(entry.get(), input)) : DataResult.error(() -> "Failed to load trap entry from JSON");
		}
		
		@NotNull
		public static Optional<TrapEntry> createFromJson(JsonOps ops, JsonElement ele)
		{
			if(ele.isJsonPrimitive())
				return CDTraps.get(Identifier.of(ele.getAsString()));
			else
			{
				JsonObject obj = ele.getAsJsonObject();
				Optional<TrapEntry> entry = CDTraps.get(Identifier.of(obj.get("Type").getAsString()));
				if(entry.isPresent())
				{
					TrapEntry ent = entry.get();
					ent.fromJson(ops, ele);
					return Optional.of(ent);
				}
			}
			return Optional.empty();
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
}
