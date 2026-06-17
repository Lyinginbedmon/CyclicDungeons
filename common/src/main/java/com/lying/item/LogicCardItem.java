package com.lying.item;

import java.util.List;

import com.lying.init.CDDataComponentTypes;
import com.lying.item.component.CircuitComponent;
import com.lying.network.ShowCircuitScreenPacket;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class LogicCardItem extends Item
{
	public LogicCardItem(Settings settings)
	{
		super(settings.component(CDDataComponentTypes.CIRCUIT.get(), CircuitComponent.empty()));
	}
	
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type)
	{
		stack.get(CDDataComponentTypes.CIRCUIT.get()).appendTooltip(context, tooltip::add, type);
	}
	
	public ActionResult use(World world, PlayerEntity user, Hand hand)
	{
		if(hand == Hand.MAIN_HAND)
		{
			if(!world.isClient())
				ShowCircuitScreenPacket.sendTo((ServerPlayerEntity)user);
			return ActionResult.SUCCESS;
		}
		return super.use(world, user, hand);
	}
}
