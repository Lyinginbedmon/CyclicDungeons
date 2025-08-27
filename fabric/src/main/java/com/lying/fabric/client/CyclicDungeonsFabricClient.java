package com.lying.fabric.client;

import com.lying.client.CyclicDungeonsClient;

import net.fabricmc.api.ClientModInitializer;

public final class CyclicDungeonsFabricClient implements ClientModInitializer
{
    public void onInitializeClient()
    {
    	CyclicDungeonsClient.clientInit();
    	
    	registerBlockColors();
    	registerParticleProviders();
    }
    
    private static void registerBlockColors()
    {
    	
    }
    
    private static void registerParticleProviders()
    {
    	
    }
}
