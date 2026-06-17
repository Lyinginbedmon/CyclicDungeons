package com.lying.client.screen.circuit;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.block.entity.logic.LogicModule;
import com.lying.client.screen.circuit.handlers.ClickHandler;
import com.lying.client.screen.circuit.handlers.ConnectPortHandler;
import com.lying.client.screen.circuit.handlers.ConnectWireHandler;
import com.lying.client.screen.circuit.handlers.GateHandler;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicCategory;
import com.lying.init.CDLogicGates.LogicGate;
import com.lying.item.component.CircuitComponent;
import com.lying.network.BuildCircuitPacket;
import com.lying.reference.Reference;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CircuitScreen extends Screen
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier TEXTURE = Reference.ModInfo.prefix("textures/gui/circuitry.png");
	public static final int GRID_SIZE	= 80;
	
	private List<Drawable> drawables = Lists.newArrayList();
	
	private Map<LogicCategory, ButtonWidget> categoryButtons = new HashMap<>();
	private Map<LogicCategory, List<ButtonWidget>> categoryMap = new HashMap<>();
	private Optional<LogicCategory> displayedCategory = Optional.empty();
	
	private Map<Vector2i, CircuitModule> circuitMap = new HashMap<>();
	private Map<String, CircuitWire> circuitWires = new HashMap<>();
	private int wireIndex = 0;
	
	// FIXME Implement naming gates & colouring wires
	private static final List<ClickHandlerBuilder> HANDLERS = Lists.newArrayList(
			ConnectPortHandler::tryCreateFrom,
			ConnectWireHandler::tryCreateFrom
			);
	private Optional<ClickHandler> currentHandler = Optional.empty();
	
	public CircuitScreen()
	{
		super(Text.empty());
		
		if(mc.player != null && mc.player.getMainHandStack().isOf(CDItems.LOGIC_CARD.get()))
		{
			CircuitComponent comp = mc.player.getMainHandStack().get(CDDataComponentTypes.CIRCUIT.get());
			comp.modules().ifPresent(set -> 
			{
				set.forEach(part -> circuitMap.put(part.pos(), new CircuitModule(part.module(), part.pos())));
				
				Map<String, CircuitWire> wireMap = new HashMap<>();
				// FIXME Generate map of wires from modules
				wireMap.entrySet().forEach(entry -> circuitWires.put(entry.getKey(), entry.getValue()));
			});
		}
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
					currentHandler = Optional.of(new GateHandler(gate, this));
					showCategory(null);
				})
						.dimensions(x, 5 + 21 + buttons.size() * 20, 60, 20)
						.build());
				button.visible = button.active = false;
				buttons.add(button);
			}
			categoryMap.put(cat, buttons);
		}
		
		addDrawableChild(ButtonWidget.builder(translate("gui","circuit_builder.build"), b -> 
		{
			BuildCircuitPacket.sendToServer(circuitMap.values().stream().map(CircuitModule::toPart).toList());
			close();
		}).dimensions(width - 60, height - 20, 60, 20).build());
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
		
		boolean canHighlightWires = currentHandler.isEmpty() || !currentHandler.get().preventsWireHighlighting();
		circuitWires.values().forEach(wire -> 
		{
			if(!wire.decapitated())
				wire.render(canHighlightWires && wire.isHovered(mouseX, mouseY, circuitMap), context, circuitMap);
		});
		
		circuitMap.values().forEach(c -> c.renderBackground(context));
		
		currentHandler.ifPresent(handler -> handler.renderBackground(context, mouseX, mouseY, delta, circuitMap));
	}
	
	protected void renderForeground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		TextRenderer textRenderer = getTextRenderer();
		circuitMap.values().forEach(c -> c.renderForeground(context, textRenderer));
		
		currentHandler.ifPresent(handler -> handler.renderForeground(context, mouseX, mouseY, delta, circuitMap));
	}
	
	public void clearHandler() { currentHandler = Optional.empty(); }
	
	public String makeWireName()
	{
		String name;
		while(circuitWires.containsKey(name = CircuitWire.getWireName(wireIndex++)))
			;
		return name;
	}
	
	public void addWire(CircuitWire wireIn)
	{
		circuitWires.put(wireIn.name(), wireIn);
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
	
	public Optional<CircuitWire> getWireAt(int mouseX, int mouseY)
	{
		return circuitWires.values().stream().filter(w -> w.isHovered(mouseX, mouseY, circuitMap)).findFirst();
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		boolean isHoldingShift = 
				InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT) || 
				InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT);

		final Vector2i gridPos = pointToGrid(mouseX, mouseY);
		final int mX = (int)mouseX, mY = (int)mouseY;
		if(button == 0)
		{
			if(currentHandler.isPresent())
				return currentHandler.get().handleClick(isHoldingShift, mX, mY, gridPos, circuitMap);
			
			for(ClickHandlerBuilder builder : HANDLERS)
			{
				Optional<ClickHandler> result = builder.tryCreateFrom(mX, mY, gridPos, circuitMap, this);
				if(result.isPresent())
				{
					currentHandler = result;
					return true;
				}
			}
		}
		else if(button == 1)
		{
			// Hide displayed category
			if(displayedCategory.isPresent())
			{
				showCategory(null);
				return true;
			}
			
			if(currentHandler.isPresent())
			{
				clearHandler();
				return true;
			}
			
			// Clearing placed gate
			final double distToSlot = gridToPoint(gridPos).distance(mX, mY);
			if(circuitMap.containsKey(gridPos) && distToSlot < GRID_SIZE / 2)
			{
				removeModule(gridPos);
				return true;
			}
			
			// Clear hovered wire
			Optional<CircuitWire> hoveredWire = getWireAt(mX, mY);
			if(hoveredWire.isPresent())
			{
				removeWire(hoveredWire.get());
				return true;
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	protected void removeModule(Vector2i gridPos)
	{
		circuitMap.remove(gridPos);
		List<CircuitWire> affectedWires = circuitWires.values().stream().filter(w -> w.isTerminus(gridPos)).toList();
		affectedWires.forEach(w -> w.removeTerminus(gridPos));
		affectedWires.stream().filter(CircuitWire::decapitated).forEach(w -> circuitWires.remove(w.name()));
	}
	
	protected void removeWire(CircuitWire wire)
	{
		final String name = wire.name();
		for(Vector2i pos : wire.getTermini())
		{
			if(!circuitMap.containsKey(pos))
				continue;
			
			CircuitModule module = circuitMap.get(pos);
			module.removeConnections(name);
		}
		circuitWires.remove(wire.name());
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
		return circuitMap.containsKey(vec) ? Optional.of(circuitMap.get(vec).module()) : Optional.empty();
	}
	
	public void put(Vector2i vec, LogicGate gate)
	{
		circuitMap.put(vec, new CircuitModule(gate.create(), vec));
	}
	
	@FunctionalInterface
	public static interface ClickHandlerBuilder
	{
		public Optional<ClickHandler> tryCreateFrom(int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, CircuitScreen screen);
	}
}
