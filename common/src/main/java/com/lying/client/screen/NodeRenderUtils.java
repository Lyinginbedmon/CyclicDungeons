package com.lying.client.screen;

import java.util.Map;
import java.util.function.Function;

import org.joml.Vector2i;

import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.Blueprint.ErrorType;
import com.lying.grammar.RoomMetadata;
import com.lying.utility.Line2f;
import com.lying.utility.Vector2iUtils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;

public class NodeRenderUtils
{
	public static final Formatting[] LINE_COLOURS = new Formatting[] 
			{
				Formatting.AQUA,
				Formatting.BLACK,
				Formatting.BLUE,
				Formatting.DARK_AQUA,
				Formatting.DARK_BLUE,
				Formatting.DARK_GRAY,
				Formatting.DARK_PURPLE,
				Formatting.DARK_RED,
				Formatting.GOLD,
				Formatting.GRAY,
				Formatting.GREEN,
				Formatting.LIGHT_PURPLE,
				Formatting.RED,
				Formatting.YELLOW,
				Formatting.WHITE
			};
	private static int colourIndex = 0;
	
	public static Function<Line2f,Line2f> scaleFunc(int renderScale, Vector2i origin)
	{
		Vec2f vec = new Vec2f(origin.x, origin.y);
		return p -> p.scale(renderScale).offset(vec);
	}
	
	public static void render(BlueprintRoom node, DrawContext context, TextRenderer textRenderer, Vector2i origin, Blueprint chart, Map<ErrorType,Integer> errors, int mouseX, int mouseY, int renderScale)
	{
		// Render node boundaries
		chart.forEach(n -> renderNodeBounds(n, origin, renderScale, context));
		// Render links between nodes
		colourIndex = 0;
		renderLinks(origin, renderScale, chart, !errors.isEmpty(), context, mouseX, mouseY);
		// Render the critical path from the start to the end of the dungeon
		if(DungeonScreen.showCriticalPath)
			renderCriticalPath(origin, renderScale, chart, context);
		// Then render icons & titles
		chart.forEach(n -> renderNode(n, origin, renderScale, context, textRenderer, mouseX, mouseY));
	}
	
	public static void renderLinks(Vector2i origin, int renderScale, Blueprint chart, boolean errorsPresent, DrawContext context, int mouseX, int mouseY)
	{
		final Function<Line2f, Line2f> scaleFunc = scaleFunc(renderScale, origin);
		DungeonScreen.totalPassages.stream()
			.forEach(p -> 
			{
				int linkColour = errorsPresent ?
						(p.hasIntersections(chart) ? 
							DungeonScreen.LIGHT_BLUE : 
							p.hasTunnels(chart) ? 
								DungeonScreen.LIME_GREEN : 
								DungeonScreen.DARK_GRAY) :
						LINE_COLOURS[colourIndex++ % LINE_COLOURS.length].getColorValue();
				 
				renderPath(p, scaleFunc, context, linkColour, p.asBox().contains(new Vec2f(mouseX, mouseY)));
			});
	}
	
	public static void renderCriticalPath(Vector2i origin, int renderScale, Blueprint chart, DrawContext context)
	{
		if(DungeonScreen.criticalPath.isEmpty())
			return;
		
		final Function<Line2f, Line2f> scaleFunc = scaleFunc(renderScale, origin);
		DungeonScreen.criticalPath.forEach(path -> renderPath(path, scaleFunc, context, DungeonScreen.GOLD, false));
	}
	
	public static void renderNodeBounds(BlueprintRoom node, Vector2i origin, int renderScale, DrawContext context)
	{
		RoomMetadata metadata = node.metadata();
		Vector2i pos = Vector2iUtils.add(origin, Vector2iUtils.mul(node.position(), renderScale));
		int iconColour = ColorHelper.withAlpha(255, metadata.type().colour());
		
		Vector2i size = metadata.size();
		context.drawBorder(pos.x - (size.x / 2) * renderScale, pos.y - (size.y / 2) * renderScale, size.x * renderScale, size.y * renderScale, iconColour);
	}
	
	public static void renderNode(BlueprintRoom node, Vector2i origin, int renderScale, DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY)
	{
		RoomMetadata metadata = node.metadata();
		Vector2i pos = Vector2iUtils.add(origin, Vector2iUtils.mul(node.position(), renderScale));
		int iconColour = ColorHelper.withAlpha(255, metadata.type().colour());
		
		context.drawTexture(RenderLayer::getGuiTextured, DungeonScreen.ICON_TEX, pos.x - 8, pos.y - 8, 0F, 0F, 16, 16, 16, 16, iconColour);
		
		if(pos.distance(mouseX, mouseY) < 8)
		{
			Text title = metadata.name();
			context.drawText(textRenderer, title, pos.x - (textRenderer.getWidth(title) / 2), pos.y - (textRenderer.fontHeight / 2), 0xFFFFFF, true);
		}
	}
	
	private static void renderLink(Vec2f start, Vec2f end, DrawContext context, int colour)
	{
		renderStraightLine(start, end, 1, context, colour);
	}
	
	private static void renderPath(BlueprintPassage path, Function<Line2f,Line2f> scaleFunc, DrawContext context, int colour, boolean showBounds)
	{
		path.asLines().stream().map(scaleFunc).forEach(l -> renderLink(l.getLeft(), l.getRight(), context, colour));
		
		if(showBounds)
			for(Line2f edge : path.asBox().asEdges())
				renderStraightLine(edge.getLeft(), edge.getRight(), 1, context, DungeonScreen.DARK_GRAY);
	}
	
	private static void renderStraightLine(Vec2f start, Vec2f end, int thickness, DrawContext context, int rgb)
	{
		Vec2f st = new Vec2f(start.x, start.y);
		Vec2f en = new Vec2f(end.x, end.y);
		Vec2f dir = en.add(st.negate());
		float len = dir.length();
		
		int col = ColorHelper.getArgb(
				255, 
				ColorHelper.getRed(rgb), 
				ColorHelper.getGreen(rgb), 
				ColorHelper.getBlue(rgb));
		
		MatrixStack matrixStack = context.getMatrices();
		matrixStack.push();
			matrixStack.translate(start.x, start.y, 0);
			matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation((float)Math.atan2(end.y - start.y, end.x - start.x)));
			context.fill(0, -thickness, (int)len, thickness, col);
		matrixStack.pop();
	}
}