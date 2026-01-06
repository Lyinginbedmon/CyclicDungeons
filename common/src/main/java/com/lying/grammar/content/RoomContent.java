package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class RoomContent
{
	private static final Map<Identifier, Supplier<? extends RoomContent>> REGISTRY	= new HashMap<>();
	public static final Codec<RoomContent> CODEC = Identifier.CODEC.comapFlatMap(id -> DataResult.success(RoomContent.get(id).get()), RoomContent::registryName);
	
	/** Default room content, does nothing */
	public static final Supplier<RoomContent> NOOP = register(() -> new RoomContent(prefix("none")) 
	{
		public void applyPreProcessing(BlueprintRoom room, RoomMetadata meta, BlueprintTileGrid tileMap, ServerWorld world) { }
		
		public void applyPostProcessing(BlockPos min, BlockPos max, ServerWorld world, BlueprintRoom room, RoomMetadata meta) { }
	});
	public static final Supplier<BattleRoomContent> BATTLE	= register(BattleRoomContent::new);
	public static final Supplier<TrapRoomContent> TRAP		= register(TrapRoomContent::new);
	
	public static <T extends RoomContent> Supplier<T> register(Supplier<T> processorIn)
	{
		REGISTRY.put(processorIn.get().registryName(), processorIn);
		return processorIn;
	}
	
	@NotNull
	public static Supplier<? extends RoomContent> get(Identifier id)
	{
		return REGISTRY.getOrDefault(id, NOOP);
	}
	
	private final Identifier id;
	
	protected RoomContent(Identifier idIn)
	{
		id = idIn;
	}
	
	public final Identifier registryName() { return id; }
	
	/** Applied before room tile generation */
	public abstract void applyPreProcessing(BlueprintRoom room, RoomMetadata meta, BlueprintTileGrid tileMap, ServerWorld world);
	
	/** Applied after all other room generation */
	public abstract void applyPostProcessing(BlockPos min, BlockPos max, ServerWorld world, BlueprintRoom room, RoomMetadata meta);
}
