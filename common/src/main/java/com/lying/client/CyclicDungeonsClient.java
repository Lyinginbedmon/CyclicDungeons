package com.lying.client;

import com.lying.CyclicDungeons;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDScreenHandlerTypes;
import com.lying.network.ShowDungeonLayoutPacket;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class CyclicDungeonsClient
{
	public static final MinecraftClient mc = MinecraftClient.getInstance();
	
	public static void clientInit()
	{
		registerRenderers();
		registerEventHandlers();
		registerClientPackets();
	}
	
	private static void registerRenderers()
	{
		
	}
	
	private static void registerEventHandlers()
	{
		
	}
	
	private static void registerClientPackets()
	{
		CyclicDungeons.LOGGER.info(" # Registered client-side packet receivers");
		
		NetworkManager.registerReceiver(NetworkManager.s2c(), ShowDungeonLayoutPacket.PACKET_TYPE, ShowDungeonLayoutPacket.PACKET_CODEC, (value, context) -> 
		{
			HandledScreens.open(CDScreenHandlerTypes.DUNGEON_LAYOUT_HANDLER.get(), mc, 0, value.title());
			if(mc.currentScreen instanceof DungeonScreen)
				((DungeonScreen)mc.currentScreen).getScreenHandler().setDisplayedGraph(value.graph());
		});
	}
}
