package com.lying.client.screen;

import java.util.List;
import java.util.Random;

import org.joml.Vector2i;

import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintOrganiser;
import com.lying.blueprint.BlueprintScruncher;
import com.lying.blueprint.Node;
import com.lying.grammar.CDMetadata;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;
import com.lying.utility.Line2;
import com.lying.utility.Vector2iUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;

public class DungeonScreen extends HandledScreen<DungeonScreenHandler>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier ICON_TEX = Reference.ModInfo.prefix("textures/gui/tree_node.png");
	public static int RENDER_SCALE = 16;
	
	private final Long randSeed;
	private Vector2i displayOffset = new Vector2i(0,0);
	private Vector2i dragStart = null;
	
	private Blueprint blueprint = null;
	
	public DungeonScreen(DungeonScreenHandler handler, PlayerInventory inventory, Text title)
	{
		super(handler, inventory, title);
		randSeed = System.currentTimeMillis();
		RENDER_SCALE = 16;
	}
	
	protected void init()
	{
		addDrawableChild(ButtonWidget.builder(Text.literal("Tree"), b -> { BlueprintOrganiser.Tree.create().organise(blueprint, new Random(randSeed)); resetDrag(); }).dimensions(0, 0, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("4x Grid"), b -> { BlueprintOrganiser.Grid.Square.create().organise(blueprint, new Random(randSeed)); resetDrag(); }).dimensions(0, 20, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("8x Grid"), b -> { BlueprintOrganiser.Grid.Octagonal.create().organise(blueprint, new Random(randSeed)); resetDrag(); }).dimensions(0, 40, 60, 20).build());
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Scrunch"), b -> BlueprintScruncher.scrunch(blueprint)).dimensions(0, 70, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Collapse"), b -> BlueprintScruncher.collapse(blueprint)).dimensions(0, 90, 60, 20).build());
	}
	
	protected void drawForeground(DrawContext context, int mouseX, int mouseY)
	{
		
	}
	
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY)
	{
		blur();
		context.drawText(textRenderer, this.title, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(this.title)) / 2, 10, 0xFFFFFF, false);
		
		final Vector2i position = isDragging() ? new Vector2i(displayOffset.x + mouseX - dragStart.x, displayOffset.y + mouseY - dragStart.y) : displayOffset;
		final Vector2i origin = new Vector2i(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 5).add(position);
		if(blueprint != null && !blueprint.isEmpty())
			NodeRenderUtils.render(blueprint.get(0), context, this.textRenderer, origin, blueprint, mouseX, mouseY, RENDER_SCALE);
	}
	
	public void handledScreenTick()
	{
		if(blueprint == null)
		{
			getScreenHandler().graph().ifPresent(graph -> 
			{
				if(graph.isEmpty())
					return;
				
				blueprint = Blueprint.fromGraph(graph);
				BlueprintOrganiser.Tree.create().organise(blueprint, new Random(randSeed));
				
				Random rand = new Random(randSeed);
				blueprint.forEach(node -> 
				{
					int x = 2 + rand.nextInt(4);
					int y = 2 + rand.nextInt(4);
					node.metadata().setSize(x, y);
				});
			});
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
		RENDER_SCALE = MathHelper.clamp(RENDER_SCALE + (int)verticalAmount, 1, 50);
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	
	public static class NodeRenderUtils
	{
		public static void render(Node node, DrawContext context, TextRenderer textRenderer, Vector2i origin, Blueprint chart, int mouseX, int mouseY, int renderScale)
		{
			// Render links between nodes first
			renderLinks(origin, renderScale, chart, context);
			// Then render icons & titles
			chart.forEach(n -> renderNode(n, origin, renderScale, context, textRenderer, mouseX, mouseY));
		}
		
		public static void renderLinks(Vector2i origin, int renderScale, Blueprint chart, DrawContext context)
		{
			boolean errorsPresent = chart.hasErrors();
			List<Line2> links = Blueprint.getAllPaths(chart);
			links.forEach(path -> 
			{
				int linkColour = 0x6E6E6E;
				if(errorsPresent)
				{
					// Path intersecting another path
					if(links.stream()
							.filter(l -> !(l.getLeft().equals(path.getLeft()) && l.getRight().equals(path.getRight())))
							.anyMatch(l -> path.intersects(l)))
						linkColour = 0x1D77F5;
					// Path intersecting an unrelated node
					else if(chart.stream()
							.filter(n -> !(n.position().equals(path.getLeft()) || n.position().equals(path.getRight())))
							.anyMatch(n -> n.bounds().intersects(path)))
						linkColour = 0x1DF537;
				}
				
				Line2 link = path.scale(renderScale).offset(origin);
				renderLink(link.getLeft(), link.getRight(), context, linkColour);
			});
		}
		
		public static void renderNode(Node node, Vector2i origin, int renderScale, DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY)
		{
			CDMetadata metadata = node.metadata();
			Vector2i pos = Vector2iUtils.add(Vector2iUtils.mul(node.position(), renderScale), origin);
			int iconColour = ColorHelper.withAlpha(255, metadata.type().colour());
			
			Vector2i size = metadata.size();
			context.drawBorder(pos.x - (size.x / 2) * renderScale, pos.y - (size.y / 2) * renderScale, size.x * renderScale, size.y * renderScale, iconColour);
			context.drawTexture(RenderLayer::getGuiTextured, DungeonScreen.ICON_TEX, pos.x - 8, pos.y - 8, 0F, 0F, 16, 16, 16, 16, iconColour);
			
			if(pos.distance(mouseX, mouseY) < 8)
			{
				Text title = metadata.name();
				context.drawText(textRenderer, title, pos.x - (textRenderer.getWidth(title) / 2), pos.y - (textRenderer.fontHeight / 2), 0xFFFFFF, true);
			}
		}
		
		private static void renderLink(Vector2i start, Vector2i end, DrawContext context, int colour)
		{
			renderStraightLine(start, end, 1, context, colour);
		}
		
		private static void renderStraightLine(Vector2i start, Vector2i end, int thickness, DrawContext context, int rgb)
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
