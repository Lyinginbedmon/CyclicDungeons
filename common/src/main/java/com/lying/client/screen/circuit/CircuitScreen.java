package com.lying.client.screen.circuit;

import static com.lying.reference.Reference.ModInfo.translate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.block.Port;
import com.lying.block.entity.logic.LogicModule;
import com.lying.client.screen.circuit.handlers.ClickHandler;
import com.lying.client.screen.circuit.handlers.ConnectPortHandler;
import com.lying.client.screen.circuit.handlers.ConnectWireHandler;
import com.lying.client.screen.circuit.handlers.PaintHandler;
import com.lying.client.screen.circuit.handlers.PlaceGateHandler;
import com.lying.client.screen.circuit.handlers.RenameGateHandler;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicCategory;
import com.lying.init.CDLogicGates.LogicGate;
import com.lying.init.CDSoundEvents;
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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CircuitScreen extends Screen
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier TEXTURE = Reference.ModInfo.prefix("textures/gui/circuitry.png");
	public static final int GRID_SIZE	= 80;
	
	private List<Drawable> drawables = Lists.newArrayList();
	public TextFieldWidget nameField;
	public Vector2i displayOffset = new Vector2i(0,0);
	private boolean dragActive = false;
	private Optional<Vector2i> dragStart = Optional.empty();
	
	private Map<LogicCategory, ButtonWidget> categoryButtons = new HashMap<>();
	private Map<LogicCategory, List<ButtonWidget>> categoryMap = new HashMap<>();
	private Optional<LogicCategory> displayedCategory = Optional.empty();
	
	private Map<Vector2i, CircuitModule> circuitMap = new HashMap<>();
	private Map<String, CircuitWire> circuitWires = new HashMap<>();
	private int wireIndex = 0;
	
	private static final List<ClickHandlerBuilder> HANDLERS = Lists.newArrayList(
			ConnectPortHandler::tryCreateFrom,
			ConnectWireHandler::tryCreateFrom,
			RenameGateHandler::tryCreateFrom
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
				circuitMap.clear();
				circuitWires.clear();
				
				// Load circuit modules from held logic card
				set.forEach(part -> circuitMap.put(part.gridPos(), new CircuitModule(part.module(), part.gridPos()).setColor(part.color().orElse(null))));
				
				// Reconstruct wiring
				for(CircuitModule module : circuitMap.values())
				{
					module.cachePortPositions();
					
					// Attach inputs of module to outputs of wires
					Map<Port, List<String>> inputs = module.collectInputWires();
					for(Port port : inputs.keySet())
						for(String name : inputs.get(port))
						{
							CircuitWire wire = circuitWires.getOrDefault(name, new CircuitWire(name));
							wire.attachPort(new CircuitPort(module.gridPosition(), port));
							circuitWires.put(name, wire);
						}
					
					// Attach outputs of module to inputs of wires
					Map<Port, List<String>> outputs = module.collectOutputWires();
					for(Port port : outputs.keySet())
						for(String name : outputs.get(port))
						{
							CircuitWire wire = circuitWires.getOrDefault(name, new CircuitWire(name));
							wire.attachPort(new CircuitPort(module.gridPosition(), port));
							circuitWires.put(name, wire);
						}
				}
				cleanCircuit();
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
			ButtonWidget category = addDrawableChild(ButtonWidget.builder(cat.displayName(), b -> { showCategory(cat); b.setFocused(false); }).dimensions(x, 5, 60, 20).build());
			categoryButtons.put(cat, category);
			
			List<ButtonWidget> buttons = Lists.newArrayList();
			for(LogicGate gate : CDLogicGates.byCategory(cat))
			{
				ButtonWidget button = addDrawableChild(ButtonWidget.builder(gate.displayName(), b -> 
				{
					currentHandler = Optional.of(new PlaceGateHandler(gate, this));
					showCategory(null);
				})
						.dimensions(x, 5 + 21 + buttons.size() * 20, 60, 20)
						.build());
				button.visible = button.active = false;
				buttons.add(button);
			}
			
			categoryMap.put(cat, buttons);
		}
		
		this.nameField = new TextFieldWidget(this.textRenderer, 0, -20, 103, 12, Text.empty()) 
		{
			public boolean charTyped(char chr, int modifiers)
			{
				// Replace all spaces with underscores
				if(chr == ' ')
					return super.charTyped('_', modifiers);
				
				// Ignore any non-alphanumeric characters
				String str = String.valueOf(chr);
				if(str.replaceAll("[^a-zA-Z0-9_]", "").length() == 0)
					return false;
				
				return super.charTyped(chr, modifiers);
			}
		};
		this.nameField.setFocusUnlocked(false);
		this.nameField.setEditableColor(-1);
		this.nameField.setUneditableColor(-1);
		this.nameField.setDrawsBackground(false);
		this.nameField.setMaxLength(15);
		this.nameField.setChangedListener(n -> currentHandler.ifPresent(h -> h.onNameChanged(n, circuitMap)));
		this.nameField.setText("");
		this.addSelectableChild(this.nameField);
		
		addDrawableChild(new IconButtonWidget(width - 22 - 5, 5, PaintHandler.TEXTURE, b -> 
		{
			currentHandler = Optional.of(new PaintHandler(this));
			showCategory(null);
		}));
		addDrawableChild(ButtonWidget.builder(translate("gui","circuit_builder.clear"), b -> 
		{
			circuitMap.clear();
			circuitWires.clear();
			displayOffset = new Vector2i(0, 0);
		}).dimensions(width - 125, height - 20, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(translate("gui","circuit_builder.build"), b -> 
		{
			cleanCircuit();
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
		if(!dragActive && dragStart.isPresent())
		{
			final int mX = (int)mouseX, mY = (int)mouseY;
			displayOffset.add(mX - dragStart.get().x(), mY - dragStart.get().y());
			dragStart = Optional.empty();
			setDragging(false);
		}
		else if(dragActive && dragStart.isEmpty())
		{
			dragStart = Optional.of(new Vector2i(mouseX, mouseY));
			setDragging(true);
		}
		
		this.renderBackground(context, mouseX, mouseY, delta);
		this.renderForeground(context, mouseX, mouseY, delta);
		this.drawables.forEach(d -> d.render(context, mouseX, mouseY, delta));
	}
	
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		super.renderBackground(context, mouseX, mouseY, delta);
		
		MatrixStack stack = context.getMatrices();
		Vector2i offset = new Vector2i(displayOffset.x(), displayOffset.y());
		dragStart.ifPresent(v -> offset.add(mouseX - v.x(), mouseY - v.y()));
		
		final int 
			mouseMicroX = mouseX - offset.x(), 
			mouseMicroY = mouseY - offset.y();
		stack.push();
			stack.translate(offset.x(), offset.y(), 0);
			if(!circuitWires.isEmpty())
				circuitWires.values().stream()
					.filter(Predicates.not(CircuitWire::decapitated))
					.forEach(wire -> wire.render(
							(dragStart.isEmpty() && (currentHandler.isEmpty() || !currentHandler.get().preventsWireHighlighting(wire))) && wire.isHovered(mouseMicroX, mouseMicroY, circuitMap), 
							context, 
							circuitMap));
			circuitMap.values().forEach(c -> c.renderBackground(context));
			currentHandler.ifPresent(handler -> handler.renderBackground(context, mouseMicroX, mouseMicroY, delta, circuitMap));
		stack.pop();
	}
	
	protected void renderForeground(DrawContext context, int mouseX, int mouseY, float delta)
	{
		TextRenderer textRenderer = getTextRenderer();
		currentHandler.ifPresent(h -> context.drawText(textRenderer, translate("gui", "circuit_builder.current_operation", h.displayName()), 5, height - (textRenderer.fontHeight + 5) * 2, -1, true));
		context.drawText(textRenderer, translate("gui", "circuit_builder.drag_key", mc.options.jumpKey.getBoundKeyLocalizedText()), 5, height - (textRenderer.fontHeight + 5), -1, true);
		
		MatrixStack stack = context.getMatrices();
		Vector2i offset = new Vector2i(displayOffset.x(), displayOffset.y());
		dragStart.ifPresent(v -> offset.add(mouseX - v.x(), mouseY - v.y()));
		
		final int 
			mouseMicroX = mouseX - offset.x(), 
			mouseMicroY = mouseY - offset.y();
		stack.push();
			stack.translate(offset.x(), offset.y(), 0);
			circuitMap.values().forEach(c -> c.renderForeground(context, textRenderer));
			currentHandler.ifPresent(handler -> handler.renderForeground(context, mouseMicroX, mouseMicroY, delta, circuitMap));
		stack.pop();
	}
	
	public void clearHandler()
	{
		currentHandler = Optional.empty();
		nameField.setFocused(false);
	}
	
	/** Cleans the connections of all modules to just those represented by wires present on the screen */
	public void cleanCircuit()
	{
		// Reset all circuit modules
		circuitMap.values().forEach(CircuitModule::clearConnections);
		
		// Repopulate connections between modules based on extant wires
		circuitWires.values().forEach(wire -> wire.assertOnCircuit(circuitMap));
		
		// Remove all circuit modules that don't have any connections
		circuitMap.values().stream().filter(CircuitModule::unconnected).toList().forEach(m -> circuitMap.remove(m.gridPosition()));
	}
	
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
	
	/** Returns the first wire (if any) at the given micro-grid position */
	public Optional<CircuitWire> getWireAt(int microX, int microY)
	{
		for(CircuitWire w : circuitWires.values())
			if(w.isHovered(microX, microY, circuitMap))
				return Optional.of(w);
		return Optional.empty();
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
		
		final int 
			mX = (int)mouseX - displayOffset.x(), 
			mY = (int)mouseY - displayOffset.y();
		final Vector2i microPos = new Vector2i(mX, mY);
		final Vector2i gridPos = microToGrid(microPos);
		
		// Left click, begin or progress an action
		if(button == 0)
		{
			if(currentHandler.isPresent())
				return currentHandler.get().handleClick(isHoldingShift, mX, mY, gridPos, circuitMap);
			
			if(super.mouseClicked(mouseX, mouseY, button))
				return true;
			else
			{
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
		}
		// Right click, cancel an action or close/delete a component
		else if(button == 1 && dragStart.isEmpty())
		{
			// Hide displayed category
			if(displayedCategory.isPresent())
			{
				showCategory(null);
				categoryButtons.values().forEach(b -> b.setFocused(false));
				return true;
			}
			
			if(currentHandler.isPresent())
			{
				clearHandler();
				return true;
			}
			
			// Clearing placed gate
			final double distToSlot = gridToMicro(gridPos).distance(microPos);
			if(circuitMap.containsKey(gridPos) && distToSlot < GRID_SIZE / 2)
			{
				playSound(SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM);
				removeModule(gridPos);
				return true;
			}
			
			// Clear hovered wire
			Optional<CircuitWire> hoveredWire = getWireAt(mX, mY);
			if(hoveredWire.isPresent())
			{
				playSound(CDSoundEvents.WIRE_DETACH.get());
				removeWire(hoveredWire.get());
				return true;
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
	{
		if(currentHandler.isPresent() && currentHandler.get().handleScroll((int)verticalAmount))
			return true;
		
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(currentHandler.isPresent() && currentHandler.get().handleKeyPress(keyCode, modifiers))
			return true;
		
		if(keyCode == GLFW.GLFW_KEY_SPACE)
		{
			setDragging(true);
			dragActive = true;
			return true;
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean keyReleased(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_SPACE)
		{
			setDragging(false);
			dragActive = false;
			return true;
		}
		
		return super.keyReleased(keyCode, scanCode, modifiers);
	}
	
	protected void removeModule(Vector2i gridPos)
	{
		circuitMap.remove(gridPos);
		for(CircuitWire wire : circuitWires.values().stream()
				.filter(w -> w.isTerminus(gridPos))
				.toList())
		{
			wire.removeTerminus(gridPos);
			if(wire.decapitated())
				removeWire(wire);
		}
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
		mc.getSoundManager().play(PositionedSoundInstance.master(CDSoundEvents.WIRE_DETACH, 1.0F));
	}
	
	public static Vector2i microToGrid(Vector2i vec)
	{
		return microToGrid(vec.x(), vec.y());
	}
	
	public static Vector2i microToGrid(double xIn, double yIn)
	{
		int x = Math.floorDiv((int)xIn, GRID_SIZE);
		int y = Math.floorDiv((int)yIn, GRID_SIZE);
		return new Vector2i(x, y);
	}
	
	public static Vector2i gridToMicro(Vector2i vec)
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
	
	public void playSound(SoundEvent sound)
	{
		mc.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0F));
	}
	
	@FunctionalInterface
	public static interface ClickHandlerBuilder
	{
		public Optional<ClickHandler> tryCreateFrom(int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, CircuitScreen screen);
	}
}
