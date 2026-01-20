package com.lying.client;

import java.util.function.BiConsumer;

import com.lying.CyclicDungeons;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDBlocks;
import com.lying.init.CDEntityTypes;
import com.lying.init.CDScreenHandlerTypes;
import com.lying.network.ShowDungeonLayoutPacket;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.world.biome.GrassColors;

public class CyclicDungeonsClient
{
	public static final MinecraftClient mc = MinecraftClient.getInstance();
	
	public static final int BASE_LEAF = -12012264;
	/** BlockColorProvider for local grass colour */
	public static final BlockColorProvider GRASS_COLOR = (state, world, pos, tintIndex) -> world != null && pos != null ? BiomeColors.getGrassColor(world, pos) : GrassColors.getDefaultColor();
	public static final int SPRUCE_LEAF = -10380959;
	/** BlockColorProvider for spruce leaf colour */
	public static final BlockColorProvider SPRUCE_COLOR = (state, world, pos, tintIndex) -> SPRUCE_LEAF;
	public static final int BIRCH_LEAF = -8345771;
	/** BlockColorProvider for birch leaf colour */
	public static final BlockColorProvider BIRCH_COLOR = (state, world, pos, tintIndex) -> BIRCH_LEAF;
	
	public static void clientInit()
	{
		registerRenderers();
		registerEventHandlers();
		registerClientPackets();
	}
	
	private static void registerRenderers()
	{
		EntityRendererRegistry.register(CDEntityTypes.DART, ArrowEntityRenderer::new);
		EntityRendererRegistry.register(CDEntityTypes.RABID_WOLF, WolfEntityRenderer::new);
		
		RenderTypeRegistry.register(RenderLayer.getCutout(), 
				CDBlocks.SENSOR_SOUND.get(), 
				CDBlocks.DART_TRAP.get());
	}
	
	public static void registerColorHandlers(BiConsumer<BlockColorProvider, Block[]> function)
	{
		function.accept(GRASS_COLOR, new Block[] { CDBlocks.GRASS_HATCH.get() });
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
