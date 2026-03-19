package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.lying.grammar.content.battle.BattleEntry;
import com.lying.init.CDBattleEntries;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;

public class BattleRoomContent extends RegistryRoomContent<BattleEntry>
{
	public static final Identifier ID	= prefix("combat_encounter");
	
	public BattleRoomContent()
	{
		super(ID);
	}
	
	public void buildRegistry(Theme theme)
	{
		theme.encounters().forEach(encounter -> register(encounter.registryName(), encounter));
	}
	
	public static class EncounterSet extends ArrayList<Identifier>
	{
		private static final long serialVersionUID = 1L;
		public static final Codec<EncounterSet> CODEC	= Identifier.CODEC.listOf().comapFlatMap(
				list -> DataResult.success(new EncounterSet(list)),
				set -> new ArrayList<Identifier>(set)
				);
		
		public EncounterSet() { }
		
		public EncounterSet(List<Identifier> entriesIn)
		{
			this();
			entriesIn.forEach(this::add);
		}
		
		public JsonElement toJson(JsonOps ops)
		{
			return CODEC.encodeStart(ops, this).getOrThrow();
		}
		
		public static EncounterSet fromJson(JsonOps ops, JsonElement ele)
		{
			return CODEC.parse(ops, ele).getOrThrow();
		}
		
		public List<BattleEntry> collect()
		{
			return stream()
				.map(CDBattleEntries.instance()::get)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		}
	}
}
