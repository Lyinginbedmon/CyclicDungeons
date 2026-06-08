package com.lying.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.block.Port;
import com.lying.block.entity.logic.LogicModule;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicCategory;
import com.lying.init.CDLogicGates.LogicGate;
import com.lying.reference.Reference;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class CircuitScreen extends Screen
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private Map<LogicCategory, ButtonWidget> categoryButtons = new HashMap<>();
	private Map<LogicCategory, List<ButtonWidget>> categoryMap = new HashMap<>();
	private Optional<LogicCategory> displayedCategory = Optional.empty();
	
	private Map<Vector2i, CircuitModule> circuitMap = new HashMap<>();
	private static final int GRID_SIZE	= 75;
	
	private Optional<LogicGate> selectedGate = Optional.empty();
	
	public CircuitScreen()
	{
		super(Reference.ModInfo.translate("gui", "circuit_screen.title"));
	}
	
	protected void init()
	{
		super.init();
		categoryButtons.clear();
		categoryMap.clear();
		for(LogicCategory cat : LogicCategory.values())
		{
			int x = 5 + categoryButtons.size() * 60;
			ButtonWidget category = addDrawableChild(ButtonWidget.builder(Text.literal(cat.name()), b -> showCategory(cat)).dimensions(x, 5, 60, 20).build());
			categoryButtons.put(cat, category);
			
			List<ButtonWidget> buttons = Lists.newArrayList();
			for(LogicGate gate : CDLogicGates.byCategory(cat))
			{
				ButtonWidget button = addDrawableChild(ButtonWidget.builder(gate.displayName(), b -> 
				{
					setGate(gate);
					showCategory(null);
				})
						.dimensions(x, 5 + 21 + buttons.size() * 20, 60, 20)
						.build());
				button.visible = button.active = false;
				buttons.add(button);
			}
			categoryMap.put(cat, buttons);
		}
	}
	
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		drawBackground(context, mouseX, mouseY, delta);
		drawForeground(context, mouseX, mouseY, delta);
	}
	
	protected void drawBackground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		super.renderBackground(context, mouseX, mouseY, delta);
		circuitMap.values().forEach(c -> c.renderBackground(context));
	}
	
	protected void drawForeground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		TextRenderer textRenderer = getTextRenderer();
		circuitMap.values().forEach(c -> c.renderForeground(context, textRenderer));
		
		selectedGate.ifPresent(g -> 
		{
			Text name = g.displayName();
			context.drawText(textRenderer, name, mouseX - (textRenderer.getWidth(name) / 2), mouseY, -1, true);
		});
	}
	
	protected void setGate(@Nullable LogicGate gate)
	{
		selectedGate = gate == null ? Optional.empty() : Optional.of(gate);
	}
	
	public void showCategory(@Nullable LogicCategory cat)
	{
		// Deactivate previous category
		displayedCategory.ifPresent(category -> 
		{
			categoryMap.get(category).forEach(b -> b.visible = b.active = false);
			categoryButtons.get(category).active = true;
		});
		
		// Activate new category
		displayedCategory = cat == null ? Optional.empty() : Optional.of(cat);
		displayedCategory.ifPresent(category -> 
		{
			categoryMap.get(category).forEach(b -> b.visible = b.active = true);
			categoryButtons.get(category).active = false;
		});
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		boolean isHoldingShift = 
				InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT) || 
				InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
		
		final Vector2i gridPos = pointToGrid(mouseX, mouseY);
		final double distToSlot = gridToPoint(gridPos).distance((int)mouseX, (int)mouseY);
		switch(button)
		{
			// Left click
			case 0:
				// Placing selected gate
				if(selectedGate.isPresent())
				{
					put(gridPos, selectedGate.get());
					if(!isHoldingShift)
						setGate(null);
					return true;
				}
				break;
			// Right click
			case 1:
				// Clearing selected gate
				if(selectedGate.isPresent())
				{
					setGate(null);
					return true;
				}
				// Clearing placed gate
				if(circuitMap.containsKey(gridPos) && distToSlot < GRID_SIZE / 2)
				{
					circuitMap.remove(gridPos);
					return true;
				}
				break;
			// Middle
			case 2:
				break;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	public static Vector2i pointToGrid(double mouseX, double mouseY)
	{
		int x = Math.floorDiv((int)mouseX, GRID_SIZE);
		int y = Math.floorDiv((int)mouseY, GRID_SIZE);
		return new Vector2i(x, y);
	}
	
	public static Vector2i gridToPoint(Vector2i vec)
	{
		return new Vector2i(
				vec.x() * GRID_SIZE + (GRID_SIZE / 2),
				vec.y() * GRID_SIZE + (GRID_SIZE / 2)
				);
	}
	
	public Optional<LogicModule> getModuleAt(Vector2i vec)
	{
		return circuitMap.containsKey(vec) ? Optional.of(circuitMap.get(vec).module) : Optional.empty();
	}
	
	public void put(Vector2i vec, LogicGate gate)
	{
		circuitMap.put(vec, new CircuitModule(gate.create(), vec));
	}
	
	protected static class CircuitModule
	{
		private final LogicModule module;
		private final Vector2i position;
		
		public CircuitModule(LogicModule moduleIn, Vector2i gridIn)
		{
			module = moduleIn;
			position = gridIn;
		}
		
		public Optional<Port> getClosestPort(int mouseX, int mouseY, boolean isOutput)
		{
			Vector2i gridPos = gridToPoint(position);
			final Vector2i mouseLocal = new Vector2i(mouseX - gridPos.x(), mouseY - gridPos.y());
			// Ignore if the mouse is outside the grid space of this module
			if(mouseLocal.length() > GRID_SIZE)
				return Optional.empty();
			
			List<Port> ports = (isOutput ? module.outputPorts() : module.inputPorts()).orElse(List.of());
			if(ports.isEmpty())
				return Optional.empty();
			
			Port closest = null;
			double minDist = Double.MAX_VALUE;
			
			final double inc = Math.toRadians(180D / (ports.size() + 1)) * (isOutput ? 1 : -1);
			final double cs = Math.cos(inc);
			final double sn = Math.sin(inc);
			Vector2i vec = new Vector2i(0, -1).mul(GRID_SIZE / 2);
			vec = new Vector2i((int)(vec.x() * cs - vec.y() * sn), (int)(vec.x() * sn + vec.y() * cs));
			for(Port port : ports)
			{
				double dist = vec.distance(mouseLocal);
				if(dist < minDist)
				{
					closest = port;
					minDist = dist;
				}
			}
			
			return closest == null ? Optional.empty() : Optional.of(closest);
		}
		
		public void renderBackground(DrawContext context)
		{
			
		}
		
		public void renderForeground(DrawContext context, TextRenderer textRenderer)
		{
			// Render display name
			Text name = module.hasCustomName() ? Text.literal(module.customName()) : module.gate().displayName();
			final Vector2i position = gridToPoint(this.position);
			final int x = position.x();
			final int y = position.y();
			context.drawText(textRenderer, name, x - (textRenderer.getWidth(name) / 2), y, -1, false);
			
			// Render inputs, if any
			module.inputPorts().ifPresent(set ->
			{
				final double inc = -Math.toRadians(180D / (set.size() + 1));
				final double cs = Math.cos(inc);
				final double sn = Math.sin(inc);
				
				Vector2i vec = new Vector2i(0, -1).mul(GRID_SIZE / 2);
				vec = new Vector2i((int)(vec.x() * cs - vec.y() * sn), (int)(vec.x() * sn + vec.y() * cs));
				for(Port port : set)
				{
					Text n = Text.literal(port.name());
					context.drawText(textRenderer, n, x + vec.x() - textRenderer.getWidth(n), y + vec.y(), -1, false);
					vec = new Vector2i((int)(vec.x() * cs - vec.y() * sn), (int)(vec.x() * sn + vec.y() * cs));
				}
			});
			
			// Render outputs, if any
			module.outputPorts().ifPresent(set -> 
			{
				final double inc = Math.toRadians(180D / (set.size() + 1));
				final double cs = Math.cos(inc);
				final double sn = Math.sin(inc);
				
				Vector2i vec = new Vector2i(0, -1).mul(GRID_SIZE / 2);
				vec = new Vector2i((int)(vec.x() * cs - vec.y() * sn), (int)(vec.x() * sn + vec.y() * cs));
				for(Port port : set)
				{
					Text n = Text.literal(port.name());
					context.drawText(textRenderer, n, x + vec.x(), y + vec.y(), -1, false);
					vec = new Vector2i((int)(vec.x() * cs - vec.y() * sn), (int)(vec.x() * sn + vec.y() * cs));
				}
			});
		}
	}
}
