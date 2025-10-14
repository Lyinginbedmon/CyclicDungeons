package com.lying.client.screen;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.Blueprint.ErrorType;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.Box2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.utility.RotaryBox2f;
import com.lying.utility.Vector2iUtils;
import com.lying.worldgen.Tile;

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
	public static final int LIGHT_BLUE = 0x1D77F5;
	public static final int LIME_GREEN = 0x1DF537;
	public static final int DARK_GRAY = 0x6E6E6E;
	public static final int GOLD = 0xFFBF00;
	public static final Formatting[] LINE_COLOURS = new Formatting[] 
			{
				Formatting.DARK_RED,
				Formatting.RED,
				Formatting.GOLD,
				Formatting.YELLOW,
				Formatting.DARK_GREEN,
				Formatting.GREEN,
				Formatting.AQUA,
				Formatting.DARK_AQUA,
				Formatting.DARK_BLUE,
				Formatting.BLUE,
				Formatting.LIGHT_PURPLE,
				Formatting.DARK_PURPLE,
				Formatting.WHITE,
				Formatting.GRAY,
				Formatting.DARK_GRAY,
				Formatting.BLACK
			};
	private static int colourIndex = 0;
	
	public static Function<LineSegment2f,LineSegment2f> scaleFunc(int renderScale, Vector2i origin)
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
		final Function<LineSegment2f, LineSegment2f> scaleFunc = scaleFunc(renderScale, origin);
		final Vec2f mousePos = new Vec2f(mouseX, mouseY);
		final Predicate<BlueprintPassage> showBounds = p -> 
		{
			CompoundBox2f box = (CompoundBox2f)p.asBox();
			for(AbstractBox2f sub : box.asBoxes())
			{
				List<Vec2f> points = Lists.newArrayList();
				points.addAll(sub.asPoints());
				
				Vec2f 
					a = points.removeFirst().multiply(renderScale).add(new Vec2f(origin.x, origin.y)),
					b = points.removeFirst().multiply(renderScale).add(new Vec2f(origin.x, origin.y)),
					c = points.removeFirst().multiply(renderScale).add(new Vec2f(origin.x, origin.y)),
					d = points.removeFirst().multiply(renderScale).add(new Vec2f(origin.x, origin.y));
				
				if(new RotaryBox2f(a, b, c, d).contains(mousePos))
					return true;
			}
			return false;
		};
		DungeonScreen.totalPassages.stream()
			.forEach(p -> 
			{
				/**
				 * Paths are rendered based on their error status
				 * * Light blue = Intersects with at least one other path
				 * * Lime green = Passes through or too close to an unrelated room boundary
				 * * Dark gray = Errors detected elsewhere in this chart
				 */
				int linkColour = errorsPresent ?
						(p.intersectsOtherPassages(chart) ? 
							LIGHT_BLUE : 
							p.intersectsOtherRooms(chart) ? 
								LIME_GREEN : 
								DARK_GRAY) :
						LINE_COLOURS[colourIndex++ % LINE_COLOURS.length].getColorValue();
				 
				renderPath(p, scaleFunc, context, linkColour, showBounds.test(p));
			});
	}
	
	public static void renderCriticalPath(Vector2i origin, int renderScale, Blueprint chart, DrawContext context)
	{
		if(DungeonScreen.criticalPath.isEmpty())
			return;
		
		final Function<LineSegment2f, LineSegment2f> scaleFunc = scaleFunc(renderScale, origin);
		DungeonScreen.criticalPath.forEach(path -> renderPath(path, scaleFunc, context, GOLD, false));
	}
	
	public static void renderNodeBounds(BlueprintRoom node, Vector2i origin, int renderScale, DrawContext context)
	{
		Vector2i pos = Vector2iUtils.add(origin, Vector2iUtils.mul(node.position(), renderScale));
		RoomMetadata metadata = node.metadata();
		Vector2i size = metadata.size();
		
		// Main bounds
		renderBoundary(pos, size, renderScale, context, ColorHelper.withAlpha(255, metadata.type().colour()));
		
		// Exterior shell
		renderBoundary(pos, Vector2iUtils.add(size, new Vector2i(2,2)), renderScale, context, ColorHelper.withAlpha(130, DARK_GRAY));
		
		// Tile grid
		int tilesX = size.x / Tile.TILE_SIZE;
		int tilesY = size.y / Tile.TILE_SIZE;
		Vector2i tile = new Vector2i(Tile.TILE_SIZE, Tile.TILE_SIZE);
		Vector2i corner = Vector2iUtils.add(pos, new Vector2i(size.x / 2 - 1, size.y / 2 - 1).negate().mul(renderScale));
		for(int x=0; x<tilesX; x++)
			for( int y=0; y<tilesY; y++)
				renderBoundary(Vector2iUtils.add(corner, new Vector2i(x, y).mul(Tile.TILE_SIZE * renderScale)), tile, renderScale, context, ColorHelper.withAlpha(75, DARK_GRAY));
	}
	
	public static void renderBoundary(Vector2i pos, Vector2i size, int renderScale, DrawContext context, int colour)
	{
		context.drawBorder(pos.x - (size.x / 2) * renderScale, pos.y - (size.y / 2) * renderScale, size.x * renderScale, size.y * renderScale, colour);
	}
	
	public static void renderBox(Box2f box, DrawContext context, int colour)
	{
		context.drawBorder((int)box.minX(), (int)box.minY(), (int)(box.maxX() - box.minX()), (int)(box.maxY() - box.minY()), colour);
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
	
	public static void renderPath(BlueprintPassage path, Function<LineSegment2f,LineSegment2f> scaleFunc, DrawContext context, int colour, boolean showBounds)
	{
		path.asLines().stream().map(scaleFunc).forEach(l -> renderStraightLine(l, context, colour));
		
		if(showBounds)
			path.asBox().asEdges().stream().map(scaleFunc).forEach(l -> renderStraightLine(l, 1, context, DARK_GRAY));
	}
	
	public static void renderStraightLine(LineSegment2f line, DrawContext context, int colour)
	{
		renderStraightLine(line, 1, context, colour);
	}
	
	public static void renderStraightLine(LineSegment2f line, int thickness, DrawContext context, int rgb)
	{
		renderStraightLine(line.getLeft(), line.getRight(), thickness, context, rgb);
	}
	
	public static void renderStraightLine(Vec2f start, Vec2f end, int thickness, DrawContext context, int rgb)
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