package com.lying.network;

import java.util.List;
import java.util.Optional;

import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.item.component.WiringComponent;
import com.lying.reference.Reference;

import dev.architectury.networking.NetworkManager;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CDPacketHandler
{
	public static final Identifier SHOW_DUNGEON_LAYOUT_ID	= make("show_dungeon_layout_screen");
	public static final Identifier CYCLE_PORT_ID			= make("cycle_port");
	
	private static Identifier make(String nameIn) { return Reference.ModInfo.prefix(nameIn); }
	
	public static void initServer()
	{
		CyclicDungeons.LOGGER.info(" # Registered server-side packet receivers");
		
		NetworkManager.registerReceiver(NetworkManager.c2s(), CyclePortPacket.PACKET_TYPE, CyclePortPacket.PACKET_CODEC, (value, context) -> 
		{
			PlayerEntity player = context.getPlayer();
			World world = player.getWorld();
			BlockPos pos = value.pos();
			Block block;
			WiringComponent wiring;
			if(
				player.getMainHandStack().isOf(CDItems.WIRING_GUN.get()) && 
				(wiring = player.getMainHandStack().get(CDDataComponentTypes.LINK_POS.get())).isWiring() &&
				(block = world.getBlockState(pos).getBlock()) instanceof IWireableBlock)
			{
				IWireableBlock wireable = (IWireableBlock)block;
				final int delta = value.delta();
				
				if(value.isOrigin())
				{
					// If the origin block only has 1 output, just ignore this
					if(wireable.outputPorts(pos, world).size() < 2)
						return;
					
					// Else, change the targeted output port
					wiring = wiring.startingAt(findNewIndex(
							Optional.of(wiring.output().get().port()), 
							wireable.outputPorts(pos, world), 
							delta));
				}
				else
				{
					// If there are only 1 or fewer input ports in the target block, just ignore this
					if(wireable.inputPorts(pos, world).size() < 2)
						return;
					
					// Else, change the targeted input port
					wiring = wiring.targeting(findNewIndex(
							wiring.input(), 
							wireable.inputPorts(pos, world), 
							delta));
				}
				
				player.getMainHandStack().set(CDDataComponentTypes.LINK_POS.get(), wiring);
			}
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
}
