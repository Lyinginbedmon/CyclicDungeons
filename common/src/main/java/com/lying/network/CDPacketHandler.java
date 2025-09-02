package com.lying.network;

import com.lying.CyclicDungeons;
import com.lying.reference.Reference;

import net.minecraft.util.Identifier;

public class CDPacketHandler
{
	public static final Identifier SHOW_DUNGEON_LAYOUT_ID	= make("show_dungeon_layout_screen");
	
	private static Identifier make(String nameIn) { return Reference.ModInfo.prefix(nameIn); }
	
	public static void initServer()
	{
		CyclicDungeons.LOGGER.info("# Registered server-side packet receivers");
	}
}
