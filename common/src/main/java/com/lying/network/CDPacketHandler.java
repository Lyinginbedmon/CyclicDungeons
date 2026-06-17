package com.lying.network;

import com.lying.CyclicDungeons;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.item.component.CircuitComponent;
import com.lying.reference.Reference;

import dev.architectury.networking.NetworkManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class CDPacketHandler
{
	public static final Identifier SHOW_DUNGEON_LAYOUT_ID	= make("show_dungeon_layout_screen");
	public static final Identifier SHOW_CIRCUIT_SCREEN_ID	= make("show_circuit_builder_screen");
	public static final Identifier PORT_SELECT_ID			= make("port_select");
	public static final Identifier BUILD_CIRCUIT_ID			= make("build_circuit");
	
	private static Identifier make(String nameIn) { return Reference.ModInfo.prefix(nameIn); }
	
	public static void initServer()
	{
		CyclicDungeons.LOGGER.info(" # Registered server-side packet receivers");
		
		NetworkManager.registerReceiver(NetworkManager.c2s(), PortSelectPacket.PACKET_TYPE, PortSelectPacket.PACKET_CODEC, (value, context) -> 
		{
			PlayerEntity player = context.getPlayer();
			World world = player.getEntityWorld();
			ItemStack item = player.getMainHandStack();
			value.applyToGun(item, player, world);
		});
		
		NetworkManager.registerReceiver(NetworkManager.c2s(), BuildCircuitPacket.PACKET_TYPE, BuildCircuitPacket.PACKET_CODEC, (value, context) -> 
		{
			PlayerEntity player = context.getPlayer();
			
			ItemStack item = player.getMainHandStack();
			boolean shouldGive = !item.isOf(CDItems.LOGIC_CARD.get());
			if(shouldGive)
				item = new ItemStack(CDItems.LOGIC_CARD.get());
			
			item.set(CDDataComponentTypes.CIRCUIT.get(), CircuitComponent.of(value.circuit()));
			
			if(shouldGive)
				player.giveItemStack(item);
		});
	}
}
