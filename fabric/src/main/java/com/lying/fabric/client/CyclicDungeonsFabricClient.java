package com.lying.fabric.client;

import com.lying.client.CyclicDungeonsClient;

import net.fabricmc.api.ClientModInitializer;

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
//    	HandledScreens.register(CDScreenHandlerTypes.DUNGEON_LAYOUT_HANDLER.get(), DungeonScreen::new);
    }
    
    private static void registerBlockColors()
    {
    	
    }
    
    private static void registerParticleProviders()
    {
    	
    }
}
