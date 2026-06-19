package com.lying.client.screen.circuit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.block.Port;
import com.lying.block.entity.logic.LogicModule;
import com.lying.block.entity.logic.PortSet;
import com.lying.client.screen.NodeRenderUtils;
import com.lying.client.screen.WiringHud;
import com.lying.item.component.CircuitComponent.CircuitPart;
import com.lying.utility.CDUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec2f;

public class CircuitModule
{
	public static final Codec<CircuitModule> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			LogicModule.CODEC.fieldOf("module").forGetter(CircuitModule::module),
			CDUtils.VEC2I_CODEC.fieldOf("grid").forGetter(c -> c.gridPosition)
			).apply(instance, (module, grid) -> new CircuitModule(module, grid)));
	private static final int gateColour = ColorHelper.withAlpha(255, 0x1D77F5);
	private static final int inputColour = ColorHelper.withAlpha(255, WiringHud.INPUT_COLOR);
	private static final int outputColour = ColorHelper.withAlpha(255, WiringHud.OUTPUT_COLOR);
	public static final Identifier TEXTURE = CircuitScreen.TEXTURE;
	private static final int PORT_SPACING	= 36;
	private static final float PORT_SCALE	=	0.75F;
	
	private final LogicModule module;
	private final Vector2i gridPosition, screenPosition;
	
	private Map<Port, Vector2i> 
			inputPortPositions = new HashMap<>(), 
			outputPortPositions = new HashMap<>();
	
	public CircuitModule(LogicModule moduleIn, Vector2i gridIn)
	{
		module = moduleIn;
		gridPosition = gridIn;
		screenPosition = CircuitScreen.gridToPoint(gridPosition);
		
		module.outputPorts().ifPresent(set -> calculatePortPositions(set, 1).forEach((key, val) -> outputPortPositions.put(key, val)));
		cachePortPositions();
	}
	
	/** Reduces this module to a server-friendly record for relay and storage */
	public CircuitPart toPart() { return new CircuitPart(module, gridPosition); }
	
	public Vector2i gridPosition() { return gridPosition; }
	
	public Vector2i screenPosition() { return screenPosition; }
	
	public LogicModule module() { return module; }
	
	public boolean unconnected() { return module.hasNoWires(); }
	
	public Map<Port, List<String>> collectInputWires()
	{
		Map<Port, List<String>> wires = new HashMap<>();
		PortSet inputs = module.inputPortSet();
		for(Port port : inputs.ports())
			wires.put(port, inputs.get(port));
		return wires;
	}
	
	public Map<Port, List<String>> collectOutputWires()
	{
		Map<Port, List<String>> wires = new HashMap<>();
		PortSet inputs = module.outputPortSet();
		for(Port port : inputs.ports())
			wires.put(port, inputs.get(port));
		return wires;
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
		if(mouseLocal.length() > CircuitScreen.GRID_SIZE)
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
	
	public void addPort(Port port, String wireName)
	{
		if(module.inputPorts().isPresent() && module.inputPorts().get().contains(port))
			addInput(port, wireName);
		else if(module.outputPorts().isPresent() && module.outputPorts().get().contains(port))
			addOutput(port, wireName);
	}
	
	public void addInput(Port input, String wireName)
	{
		module.addInput(input, wireName);
		cachePortPositions();
	}
	
	public void addOutput(Port output, String wireName)
	{
		module.addOutput(output, wireName);
	}
	
	public void removeConnections(String wireName)
	{
		if(!(module.hasInput(wireName) || module.hasOutput(wireName)))
			return;
		
		module.removeConnections(wireName);
		cachePortPositions();
	}
	
	public void clearConnections()
	{
		module.clearConnections();
		cachePortPositions();
	}
	
	/** Returns the screen position of the given port, if any */
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
		Text name = module.displayName();
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
