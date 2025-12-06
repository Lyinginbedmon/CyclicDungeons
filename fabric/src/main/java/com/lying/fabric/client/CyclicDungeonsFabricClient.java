package com.lying.fabric.client;

import com.lying.block.entity.FlameJetBlockEntity;
import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.client.CyclicDungeonsClient;
import com.lying.client.renderer.block.SightSensorBlockEntityRenderer;
import com.lying.client.renderer.block.SwingingBladeBlockEntityRenderer;
import com.lying.client.renderer.block.WireableBlockEntityRenderer;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDScreenHandlerTypes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
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
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.FLAME_JET.get(), WireableBlockEntityRenderer<FlameJetBlockEntity>::new);
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.SIGHT_SENSOR.get(), SightSensorBlockEntityRenderer::new);
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.SWINGING_BLADE.get(), SwingingBladeBlockEntityRenderer::new);
    }
    
    private static void registerScreens()
    {
    	HandledScreens.register(CDScreenHandlerTypes.DUNGEON_LAYOUT_HANDLER.get(), DungeonScreen::new);
    }
    
    private static void registerBlockColors()
    {
    	CyclicDungeonsClient.registerColorHandlers(ColorProviderRegistry.BLOCK::register);
    }
    
    private static void registerParticleProviders()
    {
    	
    }
}
