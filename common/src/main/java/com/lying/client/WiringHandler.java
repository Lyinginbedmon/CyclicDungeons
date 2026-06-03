package com.lying.client;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.lying.block.IWireableBlock;
import com.lying.block.entity.logic.PortEntry;
import com.lying.client.screen.WiringHud;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.item.WiringGunItem;
import com.lying.item.component.WiringComponent;
import com.lying.network.PortSelectPacket;
import com.lying.reference.Reference;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Controls the client-side procedure for using the wiring gun */
public class WiringHandler
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private static final WiringHud HUD = new WiringHud();
	private static Optional<BlockPos> targetWireable = Optional.empty();
	private static Optional<Integer> targetPortIndex = Optional.empty();
	
	@Nullable
	public static BlockPos targetWireable() { return targetWireable.orElse(null); }
	public static int targetIndex() { return targetPortIndex.orElse(0); }
	
	public static void setTargetWireable(BlockPos pos)
	{
		// If this is the same position we're already targeting, ignore this call
		if(
				targetWireable.isPresent() && 
				targetWireable.get().getManhattanDistance(pos) == 0)
			return;
		
		// Otherwise, set it and reset the target port index
		targetWireable = Optional.of(pos);
		targetPortIndex = Optional.of(0);
	}
	
	public static void registerEvents()
	{
		ClientGuiEvent.RENDER_HUD.register((context, tickDelta) -> 
		{
			if(!mc.isWindowFocused() || mc.currentScreen != null || mc.player == null)
				return;
			
			ItemStack stack = mc.player.getMainHandStack();
			if(!stack.isOf(CDItems.WIRING_GUN.get()))
				return;
			
			final Window window = mc.getWindow();
			HUD.init(mc, window.getScaledWidth(), window.getScaledWidth());
			HUD.render(context, 0, 0, tickDelta.getTickDelta(true));
		});
		
		// Highlighting the wireable block the player is looking at
		TickEvent.PLAYER_PRE.register((player) -> 
		{
			MinecraftClient client;
			if(!canWire(client = MinecraftClient.getInstance()))
				return;
			
			World world = player.getWorld();
			BlockPos pos;
			if(
					client.crosshairTarget.getType() == Type.BLOCK && 
					world.getBlockState(pos = ((BlockHitResult)client.crosshairTarget).getBlockPos()).getBlock() instanceof IWireableBlock)
				setTargetWireable(pos.toImmutable());
			else
				targetWireable = Optional.empty();
		});
		
		// Controlling the index of the port of the highlighted wireable block
		ClientRawInputEvent.MOUSE_SCROLLED.register((client, amountX, amountY) -> 
		{
			if(
					targetWireable.isEmpty() ||
					!canWire(client))
				return EventResult.pass();
			
			targetPortIndex = Optional.of(targetPortIndex.orElse(0) - (int)amountY);
			return EventResult.interruptTrue();
		});
		
		// Confirming the port selection and transmitting it to the server
		ClientRawInputEvent.MOUSE_CLICKED_PRE.register((client, button, action, mods) -> 
		{
			if(
					targetWireable.isEmpty() ||
					!canWire(client))
				return EventResult.pass();

			final BlockPos pos = targetWireable.get();
			PlayerEntity player = client.player;
			ItemStack stack = mc.player.getMainHandStack();
			WiringComponent comp = stack.get(CDDataComponentTypes.WIRE_OP.get());
			
			if(comp.isWiring())
			{
				// Is the targeted block the same as the first block or too far from it?
				PortEntry entry = comp.output().get();
				int dist = entry.pos().getManhattanDistance(pos);
				if(dist == 0 || dist > WiringGunItem.MAX_WIRE_RANGE)
					return EventResult.pass();
				
				// Does targeted block have no input ports?
				IWireableBlock block = IWireableBlock.getWireable(pos, player.getWorld()).get();
				if(block.inputPorts(pos, player.getWorld()).isEmpty())
					return EventResult.pass();
			}
			else
			{
				// Does targeted block have no output ports?
				IWireableBlock block = IWireableBlock.getWireable(pos, player.getWorld()).get();
				if(block.outputPorts(pos, player.getWorld()).isEmpty())
					return EventResult.pass();
			}
			
			// Send confirmation packed to server
			NetworkManager.sendToServer(new PortSelectPacket.Payload(pos, targetPortIndex.orElse(0)));
			player.getItemCooldownManager().set(player.getMainHandStack(), Reference.Values.TICKS_PER_SECOND / 2);
			targetWireable = Optional.empty();
			return EventResult.interruptTrue();
		});
	}
	
	/** Returns TRUE if the client is not distracted and the player is in appropriate condition to conduct wiring */
	public static boolean canWire(MinecraftClient client)
	{
		if(
				!client.isWindowFocused() || 
				client.currentScreen != null)
			return false;
		
		// Sneaking triggers the server-side wiring gun controls, so ignore sneaking players
		PlayerEntity player = client.player;
		if(
				player == null || 
				player.isSneaking())
			return false;
		
		ItemStack stack = player.getMainHandStack();
		if(
				!stack.isOf(CDItems.WIRING_GUN.get()) || 
				player.getItemCooldownManager().isCoolingDown(stack))
			return false;
		
		return true;
	}
}
