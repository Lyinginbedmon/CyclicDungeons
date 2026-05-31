package com.lying.client;

import java.util.Optional;

import org.slf4j.Logger;

import com.lying.block.IWireableBlock;
import com.lying.init.CDItems;
import com.lying.network.PortSelectPacket;
import com.lying.reference.Reference;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Controls the client-side procedure for using the wiring gun */
public class WiringHandler
{
	public static final Logger LOGGER = CyclicDungeonsClient.LOGGER;
	private static Optional<BlockPos> targetWireable = Optional.empty();
	private static Optional<Integer> targetPortIndex = Optional.empty();
	
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
			LOGGER.info("Processing mouse scroll, port {} selected", targetPortIndex.orElse(0));
			return EventResult.interruptTrue();
		});
		
		// Confirming the port selection and transmitting it to the server
		ClientRawInputEvent.MOUSE_CLICKED_PRE.register((client, button, action, mods) -> 
		{
			if(
					targetWireable.isEmpty() ||
					!canWire(client))
				return EventResult.pass();
			
			PlayerEntity player = client.player;
			
			// Send confirmation packed to server
			NetworkManager.sendToServer(new PortSelectPacket.Payload(targetWireable.get(), targetPortIndex.orElse(0)));
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
