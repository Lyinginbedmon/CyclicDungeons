package com.lying.blueprint.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDThemes.Theme;
import com.lying.reference.Reference;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** A room processor with a built-in typed registry that selects an entry at pre-processing and enacts it in post-processing */
public abstract class RegistryRoomProcessor<T extends IProcessorEntry> implements IRoomProcessor
{
	protected final Map<Identifier, T> registry = new HashMap<>();
	
	public final Optional<T> get(Identifier id) { return registry.containsKey(id) ? Optional.of(registry.get(id)) : Optional.empty(); }
	
	public abstract void buildRegistry(Theme theme);
	
	protected final void register(String id, T entry) { register(Reference.ModInfo.prefix(id), entry); }
	
	protected final void register(Identifier id, T entry)
	{
		registry.put(id, entry);
	}
	
	public void applyPreProcessing(BlueprintRoom room, RoomMetadata meta, BlueprintTileGrid tileMap, ServerWorld world)
	{
		registry.clear();
		buildRegistry(meta.theme());
		
		List<Identifier> ids = registry.keySet().stream().toList();
		if(!ids.isEmpty())
		{
			Identifier id = ids.size() == 1 ? ids.getFirst() : ids.get(world.random.nextInt(ids.size()));
			meta.setProcessorID(id);
			get(id).ifPresent(entry -> entry.prepare(room, tileMap, world));
		}
		
	}
	
	public void applyPostProcessing(BlockPos min, BlockPos max, ServerWorld world, BlueprintRoom room, RoomMetadata meta)
	{
		meta.processorID().ifPresent(id -> 
			registry.get(id).apply(min, max, world));
	}
}
