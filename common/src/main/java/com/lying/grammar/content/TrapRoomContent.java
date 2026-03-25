package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.trap.Trap;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTrapTypes;
import com.lying.worldgen.theme.Theme;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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
	
	public static record TrapEntry(Identifier registryName, Trap type) implements IContentEntry
	{
		public static final Codec<TrapEntry> CODEC	= Codec.of(TrapEntry::encode, TrapEntry::decode);
		
		@SuppressWarnings("unchecked")
		private static <T> DataResult<T> encode(final TrapEntry trap, final DynamicOps<T> ops, final T prefix)
		{
			if(ops != JsonOps.INSTANCE)
				return DataResult.error(() -> "Storing trap entry as NBT is not supported");
			
			return (DataResult<T>)DataResult.success(trap.toJson(JsonOps.INSTANCE));
		}
		
		private static <T> DataResult<Pair<TrapEntry, T>> decode(final DynamicOps<T> ops, final T input)
		{
			if(ops != JsonOps.INSTANCE)
				return DataResult.error(() -> "Loading trap entry from NBT is not supported");
			
			TrapEntry entry = fromJson(JsonOps.INSTANCE, (JsonObject)input);
			return entry == null ? DataResult.error(() -> "Error loading trap entry from JSON") : DataResult.success(Pair.of(entry, input));
		}
		
		/** Returns true if this entry can be applied to the given room */
		public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return type.isApplicableTo(room, meta, theme); }
		
		/** Applied when the entry is selected, before the room goes through tile generation */
		public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world) { type.prepare(room, tileMap, world); }
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { type.apply(min, max, world, meta); }
		
		public JsonObject toJson(JsonOps ops)
		{
			JsonObject obj = new JsonObject();
			obj.add("Name", Identifier.CODEC.encodeStart(ops, registryName()).getOrThrow());
			obj.add("Type", Identifier.CODEC.encodeStart(ops, type.registryName()).getOrThrow());
			type.getConfig().ifPresent(c -> obj.add("Settings", c));
			return obj;
		}
		
		@Nullable
		public static TrapEntry fromJson(JsonOps ops, JsonObject obj)
		{
			Identifier name = Identifier.CODEC.parse(ops, obj.get("Name")).getOrThrow();
			Optional<Trap> type = CDTrapTypes.get(Identifier.CODEC.parse(ops, obj.get("Type")).getOrThrow());
			if(type.isEmpty())
				return null;
			
			Trap trap = type.get();
			if(obj.has("Settings"))
				trap = trap.fromJson(ops, obj.getAsJsonObject("Settings"));
			return new TrapEntry(name, trap);
		}
	}
}
