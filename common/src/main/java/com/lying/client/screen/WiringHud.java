package com.lying.client.screen;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.PortEntry;
import com.lying.client.WiringHandler;
import com.lying.init.CDDataComponentTypes;
import com.lying.item.WiringGunItem;
import com.lying.item.component.WiringComponent;
import com.lying.utility.CDUtils;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class WiringHud extends Screen
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier[] ICON_TEX	= new Identifier[] 
			{
				prefix("textures/gui/wiring_icon_0.png"),
				prefix("textures/gui/wiring_icon_1.png"),
				prefix("textures/gui/wiring_icon_2.png")
			};
	
	private static final int OUTPUT_COLOR	= 0xD55E00;
	private static final int INPUT_COLOR	= 0x2E2585;
	private static final int SELECTED_COLOR	= 0xFFFFFF;
	private static final int OPTION_COLOR	= 0xb3b3b3;
	
	public WiringHud()
	{
		super(Text.empty());
	}
	
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		final int width = context.getScaledWindowWidth() / 2;
		final int height = context.getScaledWindowHeight() / 2;
		final TextRenderer textRenderer = getTextRenderer();
		final int fontHeight = textRenderer.fontHeight;
		
		final int index = WiringHandler.targetIndex();
		ItemStack stack = mc.player.getMainHandStack();
		WiringComponent comp = stack.get(CDDataComponentTypes.WIRE_OP.get());
		
		switch(renderText(comp, index, mc.player.isSneaking(), context, width, height, fontHeight))
		{
			default:
			case NONE:
				break;
			case SELECT_OUTPUT:
				renderIcon(0, context, width, height);
				break;
			case SHOW_OUTPUT:
				renderIcon(1, context, width, height);
				break;
			case SELECT_INPUT:
				renderIcon(2, context, width, height);
				break;
		}
	}
	
	private IconState renderText(WiringComponent comp, int index, boolean sneaking, DrawContext context, int width, int height, int fontHeight)
	{
		final int centreY = height - (fontHeight / 2);
		final int crosshairLeft = width - 20;
		
		 /** 
		 * IF WIRING
		 * 	Display selected output port on left of cursor
		 *  Display input ports of targeted block on right of cursor
		 */
		if(comp.isWiring())
		{
			// Selected output
			final Optional<PortEntry> output = comp.output();
			if(output.isEmpty())
				return IconState.NONE;
			
			final BlockPos outputPos = output.get().pos();
			renderText((sneaking ? Text.literal(outputPos.toShortString()) : comp.startName()).formatted(Formatting.BOLD), context, crosshairLeft, centreY - (fontHeight * 1), OUTPUT_COLOR, true);
			renderName(output.get().port(), context, width - 20, centreY, OUTPUT_COLOR, true);
			
			// Input wheel
			// If no target block or the same block, stop here
			final BlockPos pos = WiringHandler.targetWireable();
			int dist;
			if(pos == null || (dist = pos.getManhattanDistance(outputPos)) == 0 || dist > WiringGunItem.MAX_WIRE_RANGE)
				return IconState.SHOW_OUTPUT;
			
			IWireableBlock wireable = IWireableBlock.getWireable(pos, mc.world).get();
			List<Port> inputs = Lists.newArrayList(wireable.inputPorts(pos, mc.world));
			if(inputs.isEmpty())
				return IconState.SHOW_OUTPUT;
			
			final int crosshairRight = width + 20;
			
			Block block = mc.world.getBlockState(pos).getBlock();
			renderText((sneaking ? Text.literal(pos.toShortString()) : block.getName()).formatted(Formatting.BOLD), context, crosshairRight, centreY - (fontHeight * 2), INPUT_COLOR, false);
			inputs.sort(Port.SORT);
			renderName(CDUtils.objectFromIndex(inputs, index), context, crosshairRight, centreY, SELECTED_COLOR, false);
			if(inputs.size() > 1)
			{
				renderName(CDUtils.objectFromIndex(inputs, index - 1), context, crosshairRight + 5, centreY - fontHeight, OPTION_COLOR, false);
				renderName(CDUtils.objectFromIndex(inputs, index + 1), context, crosshairRight + 5, centreY + fontHeight, OPTION_COLOR, false);
			}
			return IconState.SELECT_INPUT;
		}
		
		/**
		 * IF NOT WIRING
		 * 	Display output ports of targeted block on left of cursor
		 */
		else
		{
			// If not wiring and no targeted block, close
			BlockPos pos = WiringHandler.targetWireable();
			if(pos == null)
				return IconState.NONE;
			
			IWireableBlock wireable = IWireableBlock.getWireable(pos, mc.world).get();
			List<Port> outputs = Lists.newArrayList(wireable.outputPorts(pos, mc.world));
			if(outputs.isEmpty())
				return IconState.NONE;
			
			Block block = mc.world.getBlockState(pos).getBlock();
			renderText((sneaking ? Text.literal(pos.toShortString()) : block.getName()).formatted(Formatting.BOLD), context, crosshairLeft, centreY - (fontHeight * 2), OUTPUT_COLOR, true);
			
			outputs.sort(Port.SORT);
			renderName(CDUtils.objectFromIndex(outputs, index), context, crosshairLeft, centreY, SELECTED_COLOR, true);
			if(outputs.size() > 1)
			{
				renderName(CDUtils.objectFromIndex(outputs, index - 1), context, crosshairLeft - 5, centreY - fontHeight, OPTION_COLOR, true);
				renderName(CDUtils.objectFromIndex(outputs, index + 1), context, crosshairLeft - 5, centreY + fontHeight, OPTION_COLOR, true);
			}
			return IconState.SELECT_OUTPUT;
		}
	}
	
	private void renderName(@Nullable Port port, DrawContext context, int x, int y, int color, boolean rightAlign)
	{
		if(port != null)
			renderText(Text.literal(port.name()), context, x, y, color, rightAlign);
	}
	
	private void renderText(Text text, DrawContext context, int x, int y, int color, boolean rightAlign)
	{
		context.drawText(getTextRenderer(), text, x - (rightAlign ? getTextRenderer().getWidth(text) : 0), y, color, false);
	}
	
	private void renderIcon(int index, DrawContext context, int width, int height)
	{
		final int scale = 25;
		context.drawTexture(
				RenderLayer::getGuiTextured, 
				ICON_TEX[index], 
				width - (scale / 2) - 1, 
				height - (scale / 2), 
				0F, 
				0F, 
				scale, 
				scale, 
				16,
				15,
				16, 
				15, 
				-1);
	}
	
	private static enum IconState
	{
		NONE,
		SELECT_OUTPUT,
		SHOW_OUTPUT,
		SELECT_INPUT;
	}
}
