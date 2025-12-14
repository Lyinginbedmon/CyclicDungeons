package com.lying.init;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.Identifier;

public class CDThemes
{
	private static final Map<Identifier, Supplier<Theme>> THEMES = new HashMap<>();
	
	public static final Supplier<Theme> BASIC	= register("basic");
	public static final Supplier<Theme> DESERT	= register("desert");
	public static final Supplier<Theme> UNDEAD	= register("undead");
	public static final Supplier<Theme> JUNGLE	= register("jungle");
	public static final Supplier<Theme> SWAMP	= register("swamp");
	
	private static Supplier<Theme> register(String nameIn)
	{
		final Identifier id = Reference.ModInfo.prefix(nameIn); 
		final Supplier<Theme> entry = () -> new Theme(id);
		THEMES.put(id, entry);
		return entry;
	}
	
	public static Optional<Theme> get(Identifier id)
	{
		return THEMES.containsKey(id) ? Optional.of(THEMES.get(id).get()) : Optional.empty();
	}
	
	public static record Theme(Identifier registryName)
	{
		public static final Codec<Theme> CODEC = Identifier.CODEC.comapFlatMap(id -> 
		{
			Optional<Theme> type = CDThemes.get(id);
			if(type.isPresent())
				return DataResult.success(type.get());
			else
				return DataResult.error(() -> "Not a recognised theme: '"+String.valueOf(id) + "'");
		}, Theme::registryName);
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} themes", THEMES.size());
	}
}
