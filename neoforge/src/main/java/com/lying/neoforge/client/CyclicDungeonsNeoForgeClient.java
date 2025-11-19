package com.lying.neoforge.client;

import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.client.CyclicDungeonsClient;
import com.lying.client.renderer.block.SightSensorBlockEntityRenderer;
import com.lying.client.renderer.block.WireableBlockEntityRenderer;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDScreenHandlerTypes;
import com.lying.reference.Reference;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Reference.ModInfo.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CyclicDungeonsNeoForgeClient
{
	@SubscribeEvent
	public static void setupClient(final FMLClientSetupEvent event)
	{
		CyclicDungeonsClient.clientInit();
    	
    	registerBlockColors();
    	
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.TRAP_LOGIC.get(), WireableBlockEntityRenderer<TrapLogicBlockEntity>::new);
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.TRAP_ACTOR.get(), WireableBlockEntityRenderer<TrapActorBlockEntity>::new);
    	BlockEntityRendererFactories.register(CDBlockEntityTypes.SIGHT_SENSOR.get(), SightSensorBlockEntityRenderer::new);
    }
    
	private static void registerBlockColors()
    {
    	MinecraftClient client = MinecraftClient.getInstance();
    	BlockColors colors = client.getBlockColors();
    }
	
	@SubscribeEvent
	private static void registerScreens(RegisterMenuScreensEvent event)
	{
		event.register(CDScreenHandlerTypes.DUNGEON_LAYOUT_HANDLER.get(), DungeonScreen::new);
	}
    
    @SubscribeEvent
    private static void registerParticleProviders(RegisterParticleProvidersEvent event)
    {
    	
    }
}
