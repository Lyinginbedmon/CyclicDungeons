package com.lying.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.lying.blueprint.Blueprint;
import com.lying.blueprint.Blueprint.ErrorType;
import com.lying.blueprint.BlueprintOrganiser;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.BlueprintScruncher;
import com.lying.grammar.RoomMetadata;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;
import com.lying.utility.Line2f;
import com.lying.utility.Vector2iUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;

public class DungeonScreen extends HandledScreen<DungeonScreenHandler>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier ICON_TEX = Reference.ModInfo.prefix("textures/gui/tree_node.png");
	public static final int LIGHT_BLUE = 0x1D77F5;
	public static final int LIME_GREEN = 0x1DF537;
	public static final int DARK_GRAY = 0x6E6E6E;
	public static final int GOLD = 0xFFBF00;
	
	public static int renderScale = 1;
	private static boolean showGoldenRoute = false;
	
	private final Long randSeed;
	private Vector2i displayOffset = new Vector2i(0,0);
	private Vector2i dragStart = null;
	
	private ButtonWidget scrunchButton, collapseButton, routeButton;
	
	private Blueprint blueprint = null;
	private Map<ErrorType,Integer> errorCache = new HashMap<>();
	
	public DungeonScreen(DungeonScreenHandler handler, PlayerInventory inventory, Text title)
	{
		super(handler, inventory, title);
		randSeed = System.currentTimeMillis();
		renderScale = 16;
		showGoldenRoute = false;
	}
	
	protected void init()
	{
		addDrawableChild(ButtonWidget.builder(Text.literal("Tree"), b -> organise(BlueprintOrganiser.Tree::create)).dimensions(0, 0, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("4x Grid"), b -> organise(BlueprintOrganiser.Grid.Square::create)).dimensions(0, 20, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("8x Grid"), b -> organise(BlueprintOrganiser.Grid.Octagonal::create)).dimensions(0, 40, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Concentric"), b -> organise(BlueprintOrganiser.Circular::create)).dimensions(0, 60, 60, 20).build());
		
		addDrawableChild(scrunchButton = ButtonWidget.builder(Text.literal("Scrunch"), b -> scrunch()).dimensions(0, 90, 60, 20).build());
		addDrawableChild(collapseButton = ButtonWidget.builder(Text.literal("Collapse"), b -> collapse()).dimensions(0, 110, 60, 20).build());
		
		addDrawableChild(routeButton = ButtonWidget.builder(Text.literal("X"), b -> showGoldenRoute = !showGoldenRoute).dimensions(width - 20, 0, 20, 20).build());
	}
	
	private void organise(Supplier<BlueprintOrganiser> organiser)
	{
		organiser.get().organise(blueprint, Random.create(randSeed));
		cacheErrors();
		resetDrag();
	}
	
	private void scrunch()
	{
		BlueprintScruncher.scrunch(blueprint, false);
		BlueprintScruncher.scrunch(blueprint, true);
		cacheErrors();
	}
	
	private void collapse()
	{
		BlueprintScruncher.collapse(blueprint, false);
		BlueprintScruncher.collapse(blueprint, true);
		cacheErrors();
	}
	
	private void cacheErrors()
	{
		for(ErrorType type : ErrorType.values())
			errorCache.put(type, Blueprint.tallyErrors(blueprint, type));
	}
	
	protected void drawForeground(DrawContext context, int mouseX, int mouseY)
	{
		context.drawText(textRenderer, this.title, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(this.title)) / 2, 10, 0xFFFFFF, false);
		
		if(blueprint != null && !blueprint.isEmpty())
		{
			int totalErrors = 0;
			for(int val : errorCache.values())
				totalErrors += val;
			
			MutableText details = totalErrors > 0 ? 
					Text.translatable("gui.cydun.dungeon_data_long", 
						blueprint.size(), 
						blueprint.maxDepth(), 
						totalErrors, 
						errorCache.get(ErrorType.COLLISION), 
						errorCache.get(ErrorType.INTERSECTION), 
						errorCache.get(ErrorType.TUNNEL)) :
					Text.translatable("gui.cydun.dungeon_data", 
						blueprint.size(), 
						blueprint.maxDepth(), 0);
			context.drawText(textRenderer, details, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(details)) / 2, 20, 0xFFFFFF, false);
		}
	}
	
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY)
	{
		blur();
		
		renderBlockGrid(context, mouseX, mouseY);
		
		if(blueprint != null && !blueprint.isEmpty())
		{
			final Vector2i position = isDragging() ? new Vector2i(displayOffset.x + mouseX - dragStart.x, displayOffset.y + mouseY - dragStart.y) : displayOffset;
			final Vector2i origin = new Vector2i(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 5).add(position);
			NodeRenderUtils.render(blueprint.get(0), context, this.textRenderer, origin, blueprint, mouseX, mouseY, renderScale);
		}
	}
	
	// TODO Render block grid under cursor
	private void renderBlockGrid(DrawContext context, int mouseX, int mouseY)
	{
//		int originX = mouseX - mouseX%renderScale;
//		int originY = mouseY - mouseY%renderScale;
//		
//		for(int i=-3; i<6; i++)
//		{
//			int off = i * renderScale;
//			NodeRenderUtils.renderStraightLine(new Vector2i(originX - off, originY), new Vector2i(originX + off, originY), 1, context, DARK_GRAY);
//			NodeRenderUtils.renderStraightLine(new Vector2i(originX, originY - off), new Vector2i(originX, originY + off), 1, context, DARK_GRAY);
//		}
	}
	
	public void handledScreenTick()
	{
		if(blueprint == null)
			getScreenHandler().graph().ifPresent(graph -> 
			{
				if(graph.isEmpty())
					return;
				
				blueprint = Blueprint.fromGraph(graph);
				blueprint.updateGoldenPath();
				Random rand = Random.create(randSeed);
				blueprint.forEach(node -> node.metadata().setSize(node.metadata().type().size(rand)));
				BlueprintOrganiser.Tree.create().organise(blueprint, rand);
				cacheErrors();
				if(blueprint.hasErrors())
					return;
			});
		else
		{
			scrunchButton.active = collapseButton.active = routeButton.active = !blueprint.hasErrors();
			showGoldenRoute = showGoldenRoute && routeButton.active;
		}
	}
	
	public void resetDrag() { this.displayOffset = new Vector2i(0,0); }
	
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button == 0)
		{
			setDragging(true);
			this.dragStart = new Vector2i((int)mouseX, (int)mouseY);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(button == 0 && isDragging())
		{
			int xOff = (int)mouseX - dragStart.x;
			int yOff = (int)mouseY - dragStart.y;
			this.displayOffset = this.displayOffset.add(xOff, yOff);
			setDragging(false);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
	{
		renderScale = MathHelper.clamp(renderScale + (int)verticalAmount, 1, 500);
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	
	public static class NodeRenderUtils
	{
		public static void render(BlueprintRoom node, DrawContext context, TextRenderer textRenderer, Vector2i origin, Blueprint chart, int mouseX, int mouseY, int renderScale)
		{
			// Render node boundaries
			chart.forEach(n -> renderNodeBounds(n, origin, renderScale, context));
			// Render links between nodes
			renderLinks(origin, renderScale, chart, context, mouseX, mouseY);
			// Render the golden path from the start to the end of the dungeon
			if(showGoldenRoute)
				renderGoldenPath(origin, renderScale, chart, context);
			// Then render icons & titles
			chart.forEach(n -> renderNode(n, origin, renderScale, context, textRenderer, mouseX, mouseY));
		}
		
		public static void renderLinks(Vector2i origin, int renderScale, Blueprint chart, DrawContext context, int mouseX, int mouseY)
		{
			boolean errorsPresent = chart.hasErrors();
			Blueprint.getPassages(chart).forEach(p -> 
			{
				int linkColour = errorsPresent ?
						(p.hasIntersections(chart) ? 
							LIGHT_BLUE : 
							p.hasTunnels(chart) ? 
								LIME_GREEN : 
								DARK_GRAY) :
						DARK_GRAY;
				
				BlueprintPassage path = new BlueprintPassage(p.asLine().scale(renderScale).offset(new Vec2f(origin.x, origin.y)), BlueprintPassage.PASSAGE_WIDTH * renderScale); 
				renderPath(path, context, linkColour, path.asBox().contains(new Vec2f(mouseX, mouseY)));
			});
		}
		
		public static void renderGoldenPath(Vector2i origin, int renderScale, Blueprint chart, DrawContext context)
		{
			if(chart.getGoldenPath().isEmpty() || chart.hasErrors())
				return;
			
			List<Line2f> links = Blueprint.getPassages(chart).stream().map(BlueprintPassage::asLine).toList();
			links.stream()
				.filter(p -> chart.getGoldenPath().stream().anyMatch(n -> n.isAt(p.getLeft()) || n.isAt(p.getRight())))
				.forEach(path -> 
				{
					Line2f link = path.scale(renderScale).offset(new Vec2f(origin.x, origin.y));
					renderLink(link.getLeft(), link.getRight(), context, GOLD);
				});
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
		
		private static void renderPath(BlueprintPassage path, DrawContext context, int colour, boolean showBounds)
		{
			renderStraightLine(path.asLine().getLeft(), path.asLine().getRight(), 1, context, colour);
			
			if(showBounds)
				for(Line2f edge : path.asBox().edges())
					renderStraightLine(edge.getLeft(), edge.getRight(), 1, context, DARK_GRAY);
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
}
