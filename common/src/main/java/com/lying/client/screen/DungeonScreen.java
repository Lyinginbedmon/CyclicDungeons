package com.lying.client.screen;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.CDMetadata;
import com.lying.grammar.CDGraph;
import com.lying.grammar.CDRoom;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class DungeonScreen extends HandledScreen<DungeonScreenHandler>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private Vector2i offset = new Vector2i(0,0);
	private Vector2i dragStart = null;
	
	private static final Identifier ICON_TEX = Reference.ModInfo.prefix("textures/gui/tree_node.png");
	
	public DungeonScreen(DungeonScreenHandler handler, PlayerInventory inventory, Text title)
	{
		super(handler, inventory, title);
	}
	
	protected void drawForeground(DrawContext context, int mouseX, int mouseY)
	{
		
	}
	
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY)
	{
		blur();
		context.drawText(textRenderer, this.title, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(this.title)) / 2, 10, 0xFFFFFF, false);
		
		final Vector2i position = isDragging() ? new Vector2i(offset.x + mouseX - dragStart.x, offset.y + mouseY - dragStart.y) : offset;
		final Vector2i origin = new Vector2i(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 5).add(position);
		getScreenHandler().graph().ifPresent(graph -> 
		{
			if(graph.isEmpty())
				return;
			
			List<Node> nodes = buildNodeGraph(graph);
			nodes.get(0).render(context, origin, nodes, mouseX, mouseY);
		});
	}
	
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
			this.offset = this.offset.add(xOff, yOff);
			setDragging(false);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	private List<Node> buildNodeGraph(CDGraph graphIn)
	{
		List<Node> graph = Lists.newArrayList();
		graphIn.getStart().ifPresent(r -> addNodeToGraph(r, null, graph, graphIn));
		return graph;
	}
	
	private Node addNodeToGraph(CDRoom room, @Nullable Node parent, List<Node> graph, CDGraph graphIn)
	{
		Node node = new Node(room.uuid(), room.metadata(), room.getLinkIds());
		graph.add(node);
		if(room.hasLinks())
			room.getLinksFrom(graphIn).forEach(r -> addNodeToGraph(r, parent, graph, graphIn));
		return node;
	}
	
	private class Node
	{
		private final UUID id;
		private final CDMetadata metadata;
		private List<UUID> links = Lists.newArrayList();
		private Vector2i position = new Vector2i(0,0);
		
		public Node(UUID idIn, CDMetadata termIn, List<UUID> linksIn)
		{
			id = idIn;
			metadata = termIn;
			links.addAll(linksIn);
		}
		
		public Node setPosition(int x, int y)
		{
			position = new Vector2i(x, y);
			return this;
		}
		
		public void render(DrawContext context, Vector2i origin, List<Node> chart, int mouseX, int mouseY)
		{
			setPosition(origin.x, origin.y);
			populatePositions(chart);
			
			// Render links first
			renderRecursive(context, chart, mouseX, mouseY, false);
			// Then render icons & titles
			renderRecursive(context, chart, mouseX, mouseY, true);
		}
		
		private void populatePositions(List<Node> chart)
		{
			if(links.isEmpty())
				return;
			
			int y = position.y + 50;
			List<Node> children = chart.stream().filter(n -> links.contains(n.id)).toList();
			
			// Total width of all children
			int x = (children.size() - 1) * 100;
			int startX = position.x - (x/2);
			for(int i=0; i<children.size(); i++)
			{
				Node child = children.get(i);
				child.setPosition(startX + 100 * i, y);
				child.populatePositions(chart);
			}
		}
		
		public void renderRecursive(DrawContext context, List<Node> chart, int mouseX, int mouseY, boolean drawIcon)
		{
			// Render node links
			if(!links.isEmpty())
				chart.stream().filter(n -> links.contains(n.id)).forEach(child -> 
				{
					// Render link to child
					if(!drawIcon)
						renderLink(position, child.position, context);
					
					child.renderRecursive(context, chart, mouseX, mouseY, drawIcon);
				});
			
			// Render node
			if(drawIcon)
			{
				context.drawTexture(RenderLayer::getGuiTextured, ICON_TEX, position.x - 8, position.y - 8, 0F, 0F, 16, 16, 16, 16, ColorHelper.withAlpha(255, metadata.type().colour()));
				
				if(position.distance(mouseX, mouseY) < 30)
				{
					Text title = metadata.name();
					context.drawText(textRenderer, title, position.x - (textRenderer.getWidth(title) / 2), position.y - (textRenderer.fontHeight / 2), 0xFFFFFF, true);
				}
			}
		}
		
		private static void renderLink(Vector2i start, Vector2i end, DrawContext context)
		{
			final int linkColor = ColorHelper.withAlpha(255, 0x6E6E6E);
			final int thickness = 1;
			if(end.x == start.x)
				renderLine(start, end, context, thickness, linkColor);
			else
			{
				Vector2i joint = new Vector2i(start.x, (start.y + end.y) / 2);
				renderLine(start, joint, context, thickness, linkColor);
				renderJointedLine(joint, end, context, thickness, linkColor);
			}
		}
		
		private static void renderJointedLine(Vector2i start, Vector2i end, DrawContext context, int border, int rgba)
		{
			Vector2i joint = new Vector2i(end.x, start.y);
			int thickness = 1;
			// Draw line from start to joint
			renderLine(start, joint, context, thickness, rgba);
			// Draw line from joint to end
			renderLine(joint, end, context, thickness, rgba);
		}
		
		private static void renderLine(Vector2i a, Vector2i b, DrawContext context, int border, int rgba)
		{
			int minX = Math.min(a.x, b.x) - border;
			int maxX = Math.max(a.x, b.x) + border;
			int minY = Math.min(a.y, b.y) - border;
			int maxY = Math.max(a.y, b.y) + border;
			context.fill(minX, minY, maxX, maxY, rgba);
		}
	}
}
