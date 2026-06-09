package com.lying.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.lying.utility.geometry.LineSegment2f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec2f;

public class CircuitScreen extends Screen
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier TEXTURE = Reference.ModInfo.prefix("textures/gui/circuitry.png");
	
	private List<Drawable> drawables = Lists.newArrayList();
	
	private Map<LogicCategory, ButtonWidget> categoryButtons = new HashMap<>();
	private Map<LogicCategory, List<ButtonWidget>> categoryMap = new HashMap<>();
	private Optional<LogicCategory> displayedCategory = Optional.empty();
	
	private Map<Vector2i, CircuitModule> circuitMap = new HashMap<>();
	private List<CircuitWire> circuitWires = Lists.newArrayList();
	private static final int GRID_SIZE	= 80;
	
	private Optional<LogicGate> selectedGate = Optional.empty();
	private Optional<CircuitPort> selectedPort = Optional.empty();
	
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
	
	protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement)
	{
		this.drawables.add(drawableElement);
		return super.addSelectableChild(drawableElement);
	}
	
	public void render(DrawContext context, int mouseX, int mouseY, float delta)
	{
		this.renderBackground(context, mouseX, mouseY, delta);
		this.renderForeground(context, mouseX, mouseY, delta);
		this.drawables.forEach(d -> d.render(context, mouseX, mouseY, delta));
	}
	
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		super.renderBackground(context, mouseX, mouseY, delta);
		
		boolean canHighlightWires = selectedGate.isEmpty() && selectedPort.isEmpty();
		circuitWires.forEach(wire -> 
		{
			if(!wire.decapitated())
				wire.render(canHighlightWires && wire.isHovered(mouseX, mouseY, circuitMap), context, circuitMap);
		});
		
		circuitMap.values().forEach(c -> c.renderBackground(context));
		
		selectedPort.ifPresent(port -> port.screenPosition(circuitMap).ifPresent(vec -> 
			NodeRenderUtils.renderStraightLine(new Vec2f(vec.x(), vec.y()), new Vec2f(mouseX, mouseY), 2, context, CircuitWire.WIRE_COLOUR)));
	}
	
	protected void renderForeground(DrawContext context, int mouseX, int mouseY, float delta)
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
				// Interact with ports
				else if(!circuitMap.isEmpty() && circuitMap.containsKey(gridPos))
				{
					if(selectedPort.isEmpty() || selectedPort.get().gridPos().gridDistance(gridPos) > 0)
					{
						CircuitModule module = circuitMap.get(gridPos);
						Optional<Port> closestPort = module.getClosestPort((int)mouseX, (int)mouseY, selectedPort.isEmpty());
						if(closestPort.isPresent())
						{
							// Try connect to first port
							if(selectedPort.isEmpty())
								closestPort.ifPresent(p -> selectedPort = Optional.of(new CircuitPort(gridPos, p)));
							// Try connect to second port
							else
								closestPort.ifPresent(p -> 
								{
									CircuitPort b = new CircuitPort(gridPos, p);
									circuitWires.add(new CircuitWire(selectedPort.get(), b));
									module.addInput(module, p);
									
									selectedPort = Optional.empty();
								});
							return true;
						}
					}
				}
				break;
			// Right click
			case 1:
				// Hide displayed category
				if(displayedCategory.isPresent())
				{
					showCategory(null);
					return true;
				}
				
				// Clearing selected gate
				if(selectedGate.isPresent())
				{
					setGate(null);
					return true;
				}
				
				// Clearing selected port
				if(selectedPort.isPresent())
				{
					selectedPort = Optional.empty();
					return true;
				}
				
				// Clearing placed gate
				if(circuitMap.containsKey(gridPos) && distToSlot < GRID_SIZE / 2)
				{
					removeModule(gridPos);
					return true;
				}
				
				// Clear hovered wire
				if(circuitWires.removeIf(w -> w.isHovered((int)mouseX, (int)mouseY, circuitMap)))
					return true;
				break;
			// Middle
			case 2:
				break;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	protected void removeModule(Vector2i gridPos)
	{
		circuitMap.remove(gridPos);
		circuitWires.removeIf(pair -> pair.removeAndDecapitate(gridPos));
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
		private static final int PORT_SPACING	= 36;
		private static final float PORT_SCALE	=	0.75F;
		
		private final LogicModule module;
		private final Vector2i gridPosition, screenPosition;
		
		private static final int gateColour = ColorHelper.withAlpha(255, 0x1D77F5);
		private static final int inputColour = ColorHelper.withAlpha(255, WiringHud.INPUT_COLOR);
		private static final int outputColour = ColorHelper.withAlpha(255, WiringHud.OUTPUT_COLOR);
		
		private Map<Port, Vector2i> 
				inputPortPositions = new HashMap<>(), 
				outputPortPositions = new HashMap<>();
		
		public CircuitModule(LogicModule moduleIn, Vector2i gridIn)
		{
			module = moduleIn;
			gridPosition = gridIn;
			screenPosition = gridToPoint(gridPosition);
			
			module.outputPorts().ifPresent(set -> calculatePortPositions(set, 1).forEach((key, val) -> outputPortPositions.put(key, val)));
			cachePortPositions();
		}
		
		protected void cachePortPositions()
		{
			inputPortPositions.clear();
			module.inputPorts().ifPresent(set -> calculatePortPositions(set, -1).forEach((key, val) -> inputPortPositions.put(key, val)));
		}
		
		protected static Map<Port, Vector2i> calculatePortPositions(List<Port> set, int clockwise)
		{
			Map<Port, Vector2i> positions = new HashMap<>();
			final double inc = Math.toRadians(180D / (set.size() + 1)) * clockwise;
			final double cs = Math.cos(inc);
			final double sn = Math.sin(inc);
			
			Vec2f vec = new Vec2f(0, -1).multiply(PORT_SPACING);
			for(int i=0; i<set.size(); i++)
			{
				vec = new Vec2f((float)(vec.x * cs - vec.y * sn), (float)(vec.x * sn + vec.y * cs));
				positions.put(set.get(i), new Vector2i((int)vec.x, (int)vec.y));
			}
			return positions;
		}
		
		/**
		 * Returns the closest port (if any) of this module to the given coordinates
		 * @param mouseX
		 * @param mouseY
		 * @param isOutput
		 * @return
		 */
		public Optional<Port> getClosestPort(int mouseX, int mouseY, boolean isOutput)
		{
			final Vector2i mouseLocal = new Vector2i(mouseX - screenPosition.x(), mouseY - screenPosition.y());
			// Ignore if the mouse is outside the grid space of this module
			if(mouseLocal.length() > GRID_SIZE)
				return Optional.empty();
			
			Map<Port, Vector2i> ports = (isOutput ? outputPortPositions : inputPortPositions);
			if(ports.isEmpty())
				return Optional.empty();
			
			Port closest = null;
			double minDist = Double.MAX_VALUE;
			for(Entry<Port, Vector2i> pair : ports.entrySet())
			{
				double dist = pair.getValue().distance(mouseLocal);
				if(dist < minDist)
				{
					closest = pair.getKey();
					minDist = dist;
				}
			}
			
			return closest == null || minDist > 10D ? Optional.empty() : Optional.of(closest);
		}
		
		public void addInput(CircuitModule source, Port output)
		{
			// FIXME Connect this module to given module
			
			cachePortPositions();
		}
		
		public Optional<Vector2i> getPortPosition(Port port)
		{
			if(inputPortPositions.containsKey(port))
				return Optional.of(inputPortPositions.get(port));
			else if(outputPortPositions.containsKey(port))
				return Optional.of(outputPortPositions.get(port));
			else
				return Optional.empty();
		}
		
		public void renderBackground(DrawContext context)
		{
			final int x = screenPosition.x();
			final int y = screenPosition.y();
			
			MatrixStack matrices = context.getMatrices();
			matrices.push();
				matrices.translate(x, y, 0);
				// Render inputs, if any
				inputPortPositions.values().forEach(vec -> 
				{
					NodeRenderUtils.renderStraightLine(Vec2f.ZERO, new Vec2f(vec.x(), vec.y()), 2, context, inputColour);
					context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, vec.x() - 10, vec.y() - 10, 48F, 0F, 20, 20, 256, 256, inputColour);
				});
				
				// Render outputs, if any
				outputPortPositions.values().forEach(vec -> 
				{
					NodeRenderUtils.renderStraightLine(Vec2f.ZERO, new Vec2f(vec.x(), vec.y()), 2, context, outputColour);
					context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, vec.x() - 10, vec.y() - 10, 48F, 0F, 20, 20, 256, 256, outputColour);
				});
			matrices.pop();
			
			context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x - 24, y - 24, 0F, 0F, 48, 48, 256, 256, gateColour);
		}
		
		public void renderForeground(DrawContext context, TextRenderer textRenderer)
		{
			// Render display name
			Text name = module.hasCustomName() ? Text.literal(module.customName()) : module.gate().displayName();
			final int x = screenPosition.x();
			final int y = screenPosition.y();
			context.drawText(textRenderer, name, x - (textRenderer.getWidth(name) / 2), y-(textRenderer.fontHeight / 2), -1, true);
			
			MatrixStack matrices = context.getMatrices();
			matrices.push();
				matrices.translate(x, y, 0);
				// Render inputs, if any
				inputPortPositions.entrySet().forEach(e -> 
				{
					Text n = Text.literal(e.getKey().name());
					Vector2i vec = e.getValue();
					renderScaledText(n, vec.x(), vec.y(), context, matrices, PORT_SCALE, textRenderer, textRenderer.getWidth(n));
				});
				
				// Render outputs, if any
				outputPortPositions.entrySet().forEach(e -> 
				{
					Text n = Text.literal(e.getKey().name());
					Vector2i vec = e.getValue();
					renderScaledText(n, vec.x(), vec.y(), context, matrices, PORT_SCALE, textRenderer, textRenderer.getWidth(n));
				});
			matrices.pop();
		}
		
		private static void renderScaledText(Text n, int x, int y, DrawContext context, MatrixStack matrices, float scale, TextRenderer textRenderer, int xOffset)
		{
			matrices.push();
				matrices.translate(x, y, 0);
				matrices.scale(scale, scale, 1F);
				context.drawText(textRenderer, n, -textRenderer.getWidth(n) / 2, -(textRenderer.fontHeight / 2), -1, false);
			matrices.pop();
		}
	}
	
	public static record CircuitPort(Vector2i gridPos, Port port)
	{
		public boolean equals(Object other)
		{
			CircuitPort otherPort;
			return other instanceof CircuitPort && (otherPort = ((CircuitPort)other)).gridPos().gridDistance(gridPos) == 0 && otherPort.port().equals(port);
		}
		
		public Optional<Vector2i> screenPosition(Map<Vector2i,CircuitModule> circuitMap)
		{
			if(!circuitMap.containsKey(gridPos))
				return Optional.empty();
			
			CircuitModule module = circuitMap.get(gridPos);
			Vector2i modPos = module.screenPosition;
			
			Optional<Vector2i> portPos = module.getPortPosition(port);
			return portPos.isEmpty() ? Optional.empty() : Optional.of(new Vector2i(modPos.x() + portPos.get().x(), modPos.y() + portPos.get().y()));
		}
	}
	
	public static class CircuitWire
	{
		public static final int WIRE_COLOUR		= 0xD0D0D0;
		public static final int WIRE_HIGHLIGHT	= 0xFFFFFF;
		private static final int WIRE_WIDTH	= 2;
		private List<CircuitPort> inputSet = Lists.newArrayList();
		private CircuitPort output;
		
		public CircuitWire(CircuitPort start, CircuitPort end)
		{
			inputSet.add(start);
			output = end;
		}
		
		public boolean removeAndDecapitate(Vector2i grid)
		{
			removeTerminus(grid);
			return decapitated();
		}
		
		public void removeTerminus(Vector2i grid)
		{
			if(output.gridPos.gridDistance(grid) == 0)
			{
				output = null;
				return;
			}
			inputSet.removeIf(in -> in.gridPos.gridDistance(grid) == 0);
		}
		
		/** Returns true if this wire has no input or output positions */
		public boolean decapitated()
		{
			return inputSet.isEmpty() || output == null;
		}
		
		public boolean isHovered(int mouseX, int mouseY, Map<Vector2i,CircuitModule> circuit)
		{
			if(decapitated())
				return false;
			
			Optional<Vector2i> startOpt = inputSet.getFirst().screenPosition(circuit);
			if(startOpt.isEmpty())
				return false;
			Optional<Vector2i> endOpt = output.screenPosition(circuit);
			if(endOpt.isEmpty())
				return false;
			
			final Vector2i start = startOpt.get();
			final Vector2i end = endOpt.get();
			final LineSegment2f line = new LineSegment2f(new Vec2f(start.x(), start.y()), new Vec2f(end.x(), end.y));
			final Vec2f normal = line.normal();
			
			final Vec2f mouse = new Vec2f(mouseX, mouseY);
			final LineSegment2f lineB = new LineSegment2f(mouse.add(normal.multiply(WIRE_WIDTH)), mouse.add(normal.negate().multiply(WIRE_WIDTH)));
			
			return line.intersectsAtAll(lineB);
		}
		
		public void render(boolean isHovered, DrawContext context, Map<Vector2i,CircuitModule> circuit)
		{
			if(decapitated())
				return;
			
			Optional<Vector2i> startOpt = inputSet.getFirst().screenPosition(circuit);
			if(startOpt.isEmpty())
				return;
			Optional<Vector2i> endOpt = output.screenPosition(circuit);
			if(endOpt.isEmpty())
				return;
			
			final Vector2i start = startOpt.get();
			final Vector2i end = endOpt.get();
			NodeRenderUtils.renderStraightLine(new Vec2f(start.x(), start.y()), new Vec2f(end.x(), end.y()), WIRE_WIDTH, context, isHovered ? WIRE_HIGHLIGHT : WIRE_COLOUR);
		}
	}
}
