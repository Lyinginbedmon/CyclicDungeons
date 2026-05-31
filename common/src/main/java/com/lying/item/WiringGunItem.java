package com.lying.item;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.List;

import com.lying.block.IWireableBlock;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDSoundEvents;
import com.lying.item.component.WireModeComponent;
import com.lying.item.component.WiringComponent;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WiringGunItem extends Item
{
	public static final double MAX_WIRE_RANGE = 32D;
	
	public WiringGunItem(Settings settings)
	{
		super(settings.component(CDDataComponentTypes.WIRE_OP.get(), WiringComponent.empty()).component(CDDataComponentTypes.WIRE_MODE.get(), new WireModeComponent(WireMode.GLOBAL)));
	}
	
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type)
	{
		stack.get(CDDataComponentTypes.WIRE_MODE.get()).appendTooltip(context, tooltip::add, type);
		stack.get(CDDataComponentTypes.WIRE_OP.get()).appendTooltip(context, tooltip::add, type);
	}
	
	public ActionResult useOnBlock(ItemUsageContext context)
	{
		World world = context.getWorld();
		// All non-sneaking control is handled client-side by the PortSelectPacket
		if(world.isClient() || !context.shouldCancelInteraction())
			return ActionResult.PASS;
		
		ItemStack stack = context.getPlayer().getMainHandStack();
		WiringComponent wiring = stack.get(CDDataComponentTypes.WIRE_OP.get());
		// If currently wiring, cancel wiring operation
		if(wiring.isWiring())
		{
			stack.set(CDDataComponentTypes.WIRE_OP.get(), WiringComponent.empty());
			context.getPlayer().sendMessage(translate("gui", "wiring_gun.cancel"), true);
			return ActionResult.SUCCESS;
		}
		// If not currently wiring
		else
		{
			final BlockPos blockPos = context.getBlockPos();
			final BlockState blockState = world.getBlockState(blockPos);
			final Block block = blockState.getBlock();
			
			// Clear settings of target block
			if(block instanceof IWireableBlock)
			{
				final MutableText blockName = block.getName().styled(s -> s.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(blockPos.toShortString()))));
				IWireableBlock wireable = IWireableBlock.getWireable(blockPos, world).get();
				final int count = wireable.wireCount(blockPos, world);
				if(count < 1)
				{
					context.getPlayer().sendMessage(translate("gui", "wiring_gun.wires_cleared.failed", blockName), true);
					return ActionResult.PASS;
				}
				wireable.clearWires(blockPos, world);
				context.getPlayer().sendMessage(translate("gui", "wiring_gun.wires_cleared.success", count, blockName), false);
				playSound(context.getPlayer(), blockPos);
				return ActionResult.SUCCESS;
			}
			// Cycle gun setting
			else
			{
				final WireMode mode = stack.get(CDDataComponentTypes.WIRE_MODE.get()).mode();
				stack.set(CDDataComponentTypes.WIRE_MODE.get(), new WireModeComponent(mode.cycle()));
				context.getPlayer().sendMessage(translate("gui", "wiring_gun.mode_change.success", mode.cycle().translate()), false);
				return ActionResult.SUCCESS;
			}
		}
	}
	
	public static void playSound(LivingEntity player, BlockPos pos)
	{
		playSound(pos, player.getEntityWorld());
	}
	
	public static void playSound(BlockPos pos, World world)
	{
		world.playSound(null, pos, CDSoundEvents.WIRING_GUN.get(), SoundCategory.PLAYERS, 1F, 0.75F + (world.getRandom().nextFloat() * 0.25F));
	}
	
	public static enum WireMode implements StringIdentifiable
	{
		GLOBAL,
		LOCAL;
		
		public static final Codec<WireMode> CODEC = StringIdentifiable.createCodec(WireMode::values);
		public static final PacketCodec<ByteBuf, WireMode> PACKET_CODEC = PacketCodecs.indexed(id -> values()[id], value -> value.ordinal());
		
		public String asString() { return name().toLowerCase(); }
		
		public static WireMode fromString(String str)
		{
			for(WireMode mode : values())
				if(mode.asString().equalsIgnoreCase(str))
					return mode;
			return GLOBAL;
		}
		
		public WireMode cycle()
		{
			return WireMode.values()[(ordinal() + 1)%WireMode.values().length];
		}
		
		public MutableText translate() { return Reference.ModInfo.translate("enum", "xyz_mode."+asString()); }
	}
}
