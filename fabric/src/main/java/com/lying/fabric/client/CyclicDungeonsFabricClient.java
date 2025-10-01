package com.lying.fabric.client;

import com.lying.client.CyclicDungeonsClient;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDScreenHandlerTypes;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class CyclicDungeonsFabricClient implements ClientModInitializer
{
    public void onInitializeClient()
    {
    	CyclicDungeonsClient.clientInit();
    	
    	registerScreens();
    	registerBlockColors();
    	registerParticleProviders();
    }
    
    private static void registerScreens()
    {
    	HandledScreens.register(CDScreenHandlerTypes.DUNGEON_LAYOUT_HANDLER.get(), DungeonScreen::new);
    }
    
    private static void registerBlockColors()
    {
    	
    }
    
    private static void registerParticleProviders()
    {
    	
    }
}
