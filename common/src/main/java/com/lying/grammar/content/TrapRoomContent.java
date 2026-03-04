package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.trap.Trap;
import com.lying.grid.BlueprintTileGrid;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
		public static final Codec<TrapEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("Name").forGetter(TrapEntry::registryName),
				Trap.CODEC.fieldOf("Type").forGetter(TrapEntry::type)
				).apply(instance, TrapEntry::new));
		
		/** Returns true if this entry can be applied to the given room */
		public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return type.isApplicableTo(room, meta, theme); }
		
		/** Applied when the entry is selected, before the room goes through tile generation */
		public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world) { type.prepare(room, tileMap, world); }
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { type.apply(min, max, world, meta); }
	}
}
