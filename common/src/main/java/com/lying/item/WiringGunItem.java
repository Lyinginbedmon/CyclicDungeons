package com.lying.item;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.List;

import com.lying.block.IWireableBlock;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDSoundEvents;
import com.lying.item.component.WiringComponent;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WiringGunItem extends Item
{
	public static final double MAX_WIRE_RANGE = 32D;
	
	public WiringGunItem(Settings settings)
	{
		super(settings.component(CDDataComponentTypes.LINK_POS.get(), WiringComponent.empty()));
	}
	
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type)
	{
		stack.get(CDDataComponentTypes.LINK_POS.get()).appendTooltip(context, tooltip::add, type);
	}
	
	public ActionResult useOnBlock(ItemUsageContext context)
	{
		World world = context.getWorld();
		if(world.isClient())
			return ActionResult.PASS;
		
		ItemStack stack = context.getPlayer().getStackInHand(context.getHand());
		WiringComponent wiring = stack.get(CDDataComponentTypes.LINK_POS.get());
		
		BlockPos blockPos = context.getBlockPos();
		Block block = world.getBlockState(blockPos).getBlock();
		final MutableText blockName = block.getName().styled(s -> s.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(blockPos.toShortString()))));
		
		// If currently wiring
		if(wiring.isWiring())
		{
			BlockPos linkPos = wiring.pos().get();
			final MutableText linkName = world.getBlockState(linkPos).getBlock().getName().styled(s -> s.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(linkPos.toShortString()))));
			
			// If target isn't wireable || sneaking = Cancel wiring
			if(!(block instanceof IWireableBlock) || context.shouldCancelInteraction())
			{
				stack.set(CDDataComponentTypes.LINK_POS.get(), WiringComponent.empty());
				context.getPlayer().sendMessage(translate("gui", "wiring_gun.cancel"), true);
				return ActionResult.SUCCESS;
			}
			// If too far away = Prevent wiring
			else if(!wiring.pos().get().isWithinDistance(blockPos, MAX_WIRE_RANGE))
			{
				context.getPlayer().sendMessage(translate("gui", "wiring_gun.out_of_range", linkName), true);
				return ActionResult.PASS;
			}
			// Else wire to target block
			else
			{
				// Feed position to block, clear from gun if success
				IWireableBlock wireable = (IWireableBlock)block;
				if(wireable.acceptWireTo(wiring.type().get(), linkPos, blockPos, world))
				{
					stack.set(CDDataComponentTypes.LINK_POS.get(), WiringComponent.empty());
					
					context.getPlayer().sendMessage(translate("gui", "wiring_gun.success", linkName, blockName), false);
					playSound(context.getPlayer(), blockPos);
					return ActionResult.SUCCESS;
				}
			}
		}
		// If not currently wiring
		else
		{
			// Sneaking = Clear attached wires
			if(context.shouldCancelInteraction())
			{
				// Clear settings of target block
				IWireableBlock wireable = IWireableBlock.getWireable(blockPos, world);
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
			// Otherwise = Start wiring
			else if(block instanceof IWireableBlock)
			{
				// Store block in position
				context.getPlayer().sendMessage(translate("gui", "wiring_gun.start", blockName), true);
				stack.set(CDDataComponentTypes.LINK_POS.get(), WiringComponent.of(blockPos, IWireableBlock.getWireable(blockPos, world).type()));
				playSound(context.getPlayer(), blockPos);
				return ActionResult.SUCCESS;
			}
			else
			{
				context.getPlayer().sendMessage(translate("gui", "wiring_gun.failed", blockName), true);
				return ActionResult.FAIL;
			}
		}
		
		return ActionResult.PASS;
	}
	
	private static void playSound(LivingEntity player, BlockPos pos)
	{
		player.getEntityWorld().playSound(null, pos, CDSoundEvents.WIRING_GUN.get(), SoundCategory.PLAYERS, 1F, 0.75F + (player.getRandom().nextFloat() * 0.25F));
	}
}
