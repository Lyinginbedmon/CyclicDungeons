package com.lying.neoforge.client;

import com.lying.client.CyclicDungeonsClient;
import com.lying.reference.Reference;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CyclicDungeonsNeoForgeClient
{
	@SubscribeEvent
	public static void setupClient(final FMLClientSetupEvent event)
	{
		CyclicDungeonsClient.clientInit();
    	
    	registerBlockColors();
    }
    
	private static void registerBlockColors()
    {
    	MinecraftClient client = MinecraftClient.getInstance();
    	BlockColors colors = client.getBlockColors();
    }
    
    @SubscribeEvent
    private static void registerParticleProviders(RegisterParticleProvidersEvent event)
    {
    	
    }
}
