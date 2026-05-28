package com.lying.client;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.Port;
import com.lying.client.screen.DungeonScreen;
import com.lying.init.CDBlocks;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDEntityTypes;
import com.lying.init.CDItems;
import com.lying.init.CDScreenHandlerTypes;
import com.lying.item.component.WiringComponent;
import com.lying.network.ShowDungeonLayoutPacket;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
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
import net.minecraft.client.render.entity.PolarBearEntityRenderer;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
		EntityRendererRegistry.register(CDEntityTypes.RABID_POLAR_BEAR, PolarBearEntityRenderer::new);
		
		RenderTypeRegistry.register(RenderLayer.getCutout(), 
				CDBlocks.SENSOR_SOUND.get(), 
				CDBlocks.DART_TRAP.get(),
				CDBlocks.SPIKES.get(),
				CDBlocks.SPAWNER.get());
	}
	
	public static void registerColorHandlers(BiConsumer<BlockColorProvider, Block[]> function)
	{
		function.accept(GRASS_COLOR, new Block[] { CDBlocks.GRASS_HATCH.get() });
	}
	
	private static void registerEventHandlers()
	{
		ClientRawInputEvent.MOUSE_SCROLLED.register((client, amountX, amountY) -> 
		{
			if(!client.isWindowFocused() || client.player == null || client.currentScreen != null)
				return EventResult.pass();
			
			// Are we holding a wiring gun that is actively wiring and looking at a wireable block?
			World world = client.player.getWorld();
			BlockPos pos;
			Block block;
			WiringComponent wiring;
			if(
				client.player.getMainHandStack().isOf(CDItems.WIRING_GUN.get()) && 
				(wiring = client.player.getMainHandStack().get(CDDataComponentTypes.LINK_POS.get())).isWiring() &&
				client.crosshairTarget.getType() == Type.BLOCK && 
				(block = world.getBlockState(pos = ((BlockHitResult)client.crosshairTarget).getBlockPos()).getBlock()) instanceof IWireableBlock)
			{
				// FIXME Convert to a packet handled server-side to ensure proper function in multiplayer
				
				IWireableBlock wireable = (IWireableBlock)block;
				int delta = (int)amountY;
				if(wiring.output().get().pos().getManhattanDistance(pos) == 0)
				{
					// If the origin block only has 1 output, just ignore this
					if(wireable.outputPorts(pos, world).size() < 2)
						return EventResult.pass();
					
					// If we're looking at the origin, change the output port
					wiring = wiring.startingAt(findNewIndex(
							Optional.of(wiring.output().get().port()), 
							wireable.outputPorts(pos, world), 
							delta));
				}
				else
				{
					// If there are only 1 or fewer input ports in the target block, just ignore this
					if(wireable.inputPorts(pos, world).size() < 2)
						return EventResult.pass();
					
					// Else, change the targeted input port
					wiring = wiring.targeting(findNewIndex(
							wiring.input(), 
							wireable.inputPorts(pos, world), 
							delta));
				}
				
				client.player.getMainHandStack().set(CDDataComponentTypes.LINK_POS.get(), wiring);
				return EventResult.interruptTrue();
			}
			return EventResult.pass();
		});
	}
	
	private static Port findNewIndex(Optional<Port> current, List<Port> ports, final int delta)
	{
		if(ports.size() == 1)
			return ports.getFirst();
		
		int index = delta;
		if(current.isPresent())
		{
			// Try to find current index
			for(int i=0; i<ports.size(); i++)
				if(ports.get(i).equals(current.get()))
				{
					index += i;
					break;
				}
		}
		while(index < 0)
			index += ports.size();
		return ports.get(index % ports.size());
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
