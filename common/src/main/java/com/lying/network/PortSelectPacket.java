package com.lying.network;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.PortEntry;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.item.WiringGunItem;
import com.lying.item.component.WiringComponent;
import com.lying.utility.CDUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortSelectPacket
{
	private static final Identifier PACKET_ID = CDPacketHandler.PORT_SELECT_ID;
	public static final CustomPayload.Id<Payload> PACKET_TYPE	= new CustomPayload.Id<>(PACKET_ID);
	public static final PacketCodec<RegistryByteBuf, Payload> PACKET_CODEC	= CustomPayload.codecOf(Payload::write, Payload::read);
	
	public static class Payload implements CustomPayload
	{
		private final int index;
		private final BlockPos pos;
		
		public Payload(BlockPos position, int amount)
		{
			index = amount;
			pos = position;
		}
		
		public static Payload read(RegistryByteBuf buffer)
		{
			return new Payload(
					new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()), 
					buffer.readInt());
		}
		
		public void write(RegistryByteBuf buffer)
		{
			buffer.writeInt(pos.getX());
			buffer.writeInt(pos.getY());
			buffer.writeInt(pos.getZ());
			buffer.writeInt(index);
		}
		
		public Id<? extends CustomPayload> getId() { return PACKET_TYPE; }
		
		public void applyToGun(ItemStack wireGun, PlayerEntity player, World world)
		{
			if(!wireGun.isOf(CDItems.WIRING_GUN.get()))
				return;
			
			WiringComponent wiring = wireGun.get(CDDataComponentTypes.WIRE_OP.get());
			final MutableText blockName = CDUtils.blockWithPos(pos, world);
			
			// Inert, set output port
			if(!wiring.isWiring())
			{
				IWireableBlock wireable = (IWireableBlock)world.getBlockState(pos).getBlock();
				List<Port> ports = Lists.newArrayList(wireable.outputPorts(pos, world));
				if(ports.isEmpty())
				{
					player.sendMessage(translate("gui", "wiring_gun.no_outputs", blockName), true);
					return;
				}
				ports.sort(Port.SORT);
				
				PortEntry port = new PortEntry(pos, CDUtils.objectFromIndex(ports, index));
				confirm(wireGun, WiringComponent.of(port, blockName), pos, world);
				player.sendMessage(translate("gui", "wiring_gun.start", port.port().name(), blockName), true);
			}
			// Output already selected, select input port & finalise
			else
			{
				// Wiring a block to itself is not permitted
				if(pos.getManhattanDistance(wiring.output().get().pos()) == 0)
				{
					player.sendMessage(translate("gui", "wiring_gun.cannot_self_wire"), true);
					return;
				}
				
				// Wiring blocks beyond 32 blocks apart is not permitted
				if(pos.getManhattanDistance(wiring.output().get().pos()) > WiringGunItem.MAX_WIRE_RANGE)
				{
					player.sendMessage(translate("gui", "wiring_gun.out_of_range", blockName), true);
					return;
				}
				
				IWireableBlock wireable = (IWireableBlock)world.getBlockState(pos).getBlock();
				List<Port> ports = Lists.newArrayList(wireable.inputPorts(pos, world));
				if(ports.isEmpty())
				{
					player.sendMessage(translate("gui", "wiring_gun.no_inputs", blockName), true);
					return;
				}
				ports.sort(Port.SORT);
				
				PortEntry end = new PortEntry(pos, CDUtils.objectFromIndex(ports, index));
				PortEntry start = wiring.output().get();
				
				if(WiringComponent.tryWire(start, end, world, wireGun.get(CDDataComponentTypes.WIRE_MODE.get()).mode()))
				{
					confirm(wireGun, WiringComponent.empty(), pos, world);
					player.sendMessage(translate("gui", "wiring_gun.success", 
							start.port().name(), 
							wiring.startName(), 
							end.port().name(), 
							blockName), true);
				}
			}
		}
		
		private static void confirm(ItemStack gun, WiringComponent comp, BlockPos pos, World world)
		{
			gun.set(CDDataComponentTypes.WIRE_OP.get(), comp);
			WiringGunItem.playSound(pos, world);
		}
	}
}
