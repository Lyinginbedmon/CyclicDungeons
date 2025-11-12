package com.lying.item;

import java.util.Optional;

import com.lying.block.IWireableBlock;
import com.lying.init.CDDataComponentTypes;
import com.lying.item.component.WiringComponent;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class WireGunItem extends Item
{
	public WireGunItem(Settings settings)
	{
		super(settings.component(CDDataComponentTypes.LINK_POS.get(), WiringComponent.empty()));
	}
	
	public ActionResult useOnBlock(ItemUsageContext context)
	{
		if(context.getWorld().isClient())
			return ActionResult.PASS;
		
		ItemStack stack = context.getPlayer().getStackInHand(context.getHand());
		WiringComponent wiring = stack.get(CDDataComponentTypes.LINK_POS.get());
		
		BlockPos blockPos = context.getBlockPos();
		Block block = context.getWorld().getBlockState(blockPos).getBlock();
		if(wiring.isWiring())
		{
			boolean isWireable = block instanceof IWireableBlock;
			if(!isWireable && context.shouldCancelInteraction())
			{
				// Clear wiring activity
				stack.set(CDDataComponentTypes.LINK_POS.get(), WiringComponent.empty());
				context.getPlayer().sendMessage(Text.literal("Wiring cancelled"), false);
				return ActionResult.SUCCESS;
			}
			else if(!wiring.pos().get().isWithinDistance(blockPos, 32))
			{
				return ActionResult.PASS;
			}
			else if(isWireable)
			{
				// Feed position to block, clear from gun if success
				Optional<BlockPos> linkPos = wiring.pos();
				IWireableBlock wireable = (IWireableBlock)block;
				if(wireable.acceptWireTo(wiring.type().get(), linkPos.get(), blockPos, context.getWorld()))
				{
					stack.set(CDDataComponentTypes.LINK_POS.get(), WiringComponent.empty());
					context.getPlayer().sendMessage(Text.literal("Position connected"), false);
					return ActionResult.SUCCESS;
				}
			}
		}
		else
		{
			if(context.shouldCancelInteraction())
			{
				// Clear settings of target block
				IWireableBlock.getWireable(blockPos, context.getWorld()).clearWires(blockPos, context.getWorld());
				
				context.getPlayer().sendMessage(Text.literal("All wires cleared"), false);
				return ActionResult.SUCCESS;
			}
			else
			{
				// Store block in position
				context.getPlayer().sendMessage(Text.literal("Position logged"), false);
				stack.set(CDDataComponentTypes.LINK_POS.get(), WiringComponent.of(blockPos, IWireableBlock.getWireable(blockPos, context.getWorld()).type()));
				return ActionResult.SUCCESS;
			}
		}
		
		return ActionResult.PASS;
	}
}
