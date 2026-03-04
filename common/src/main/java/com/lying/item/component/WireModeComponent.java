package com.lying.item.component;

import java.util.function.Consumer;

import com.lying.item.WiringGunItem.WireMode;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;

public record WireModeComponent(WireMode mode) implements TooltipAppender
{
	public static final Codec<WireModeComponent> CODEC	= WireMode.CODEC.comapFlatMap(m -> DataResult.success(new WireModeComponent(m)), WireModeComponent::mode);
	public static final PacketCodec<ByteBuf, WireModeComponent> PACKET_CODEC	= WireMode.PACKET_CODEC.xmap(WireModeComponent::new, WireModeComponent::mode);
	
	public void appendTooltip(TooltipContext context, Consumer<Text> tooltip, TooltipType type)
	{
		tooltip.accept(Reference.ModInfo.translate("gui", "wiring_gun.xyz_mode", mode.translate()));
	}
}
