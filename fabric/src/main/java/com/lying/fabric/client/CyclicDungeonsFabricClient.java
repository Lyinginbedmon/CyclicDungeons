package com.lying.fabric.client;

import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.client.CyclicDungeonsClient;
import com.lying.client.renderer.block.SightSensorBlockEntityRenderer;
import com.lying.client.renderer.block.WireableBlockEntityRenderer;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDScreenHandlerTypes;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class CyclicDungeonsFabricClient implements ClientModInitializer
{
    public void onInitializeClient()
    {
    	CyclicDungeonsClient.clientInit();
    	
    	registerScreens();
    	registerBlockColors();
    	registerParticleProviders();
    	
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.TRAP_LOGIC.get(), WireableBlockEntityRenderer<TrapLogicBlockEntity>::new);
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.TRAP_ACTOR.get(), WireableBlockEntityRenderer<TrapActorBlockEntity>::new);
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.SIGHT_SENSOR.get(), SightSensorBlockEntityRenderer::new);
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
