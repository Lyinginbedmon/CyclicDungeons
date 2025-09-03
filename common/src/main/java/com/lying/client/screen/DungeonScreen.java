package com.lying.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.grammar.CDGraph;
import com.lying.grammar.CDMetadata;
import com.lying.grammar.CDRoom;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;

public class DungeonScreen extends HandledScreen<DungeonScreenHandler>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private Vector2i offset = new Vector2i(0,0);
	private Vector2i dragStart = null;
	
	private List<Node> chart = null;
	
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
		if(chart != null && !chart.isEmpty())
			chart.get(0).render(context, origin, chart, mouseX, mouseY);
	}
	
	public void handledScreenTick()
	{
		if(chart == null)
		{
			getScreenHandler().graph().ifPresent(graph -> 
			{
				if(graph.isEmpty())
					return;
				
				chart = buildNodeGraph(graph);
//				organisePositionsByRow(chart);
				organisePositionsByGrid(chart);
			});
		}
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
		Node node = new Node(room.uuid(), room.metadata(), room.getChildLinks(), room.getParentLinks());
		graph.add(node);
		if(room.hasLinks())
			room.getChildRooms(graphIn).forEach(r -> addNodeToGraph(r, parent, graph, graphIn));
		return node;
	}
	
	private static void organisePositionsByRow(List<Node> chart)
	{
		Map<Integer, List<Node>> depthMap = new HashMap<>();
		for(Node node : chart)
		{
			int depth = node.metadata.depth();
			List<Node> nodes = depthMap.getOrDefault(depth, Lists.newArrayList());
			nodes.add(node);
			depthMap.put(depth, nodes);
		}
		
		for(Entry<Integer, List<Node>> row : depthMap.entrySet())
		{
			List<Node> nodes = row.getValue();
			int y = row.getKey() * 50;
			int rowWidth = (nodes.size() - 1) * 100;
			for(int i=0; i<nodes.size(); i++)
				nodes.get(i).setPosition(-(rowWidth / 2) + (i * 100), y);
		}
	}
	
	private static void organisePositionsByGrid(List<Node> chart)
	{
		Map<Vector2i, Node> gridMap = new HashMap<>();
		
		final int gridSize = 50;
		final Vector2i[] neighbours = new Vector2i[]
				{
					new Vector2i(gridSize, 0),
					new Vector2i(-gridSize, 0),
					new Vector2i(0, gridSize),
					new Vector2i(0, -gridSize)
				};
		
		int maxDepth = 0;
		for(Node node : chart)
			if(node.metadata.depth() > maxDepth)
				maxDepth = node.metadata.depth();
		
		Random rand = new Random();
		for(int step = 0; step <= maxDepth; step++)
			organiseByGrid(chart, step, gridMap, neighbours, rand);
		
		if(gridMap.size() != chart.size())
			CyclicDungeons.LOGGER.warn("Grid layout size ({}) differs from input graph size ({})", gridMap.size(), chart.size());
	}
	
	private static void organiseByGrid(List<Node> chart, int depth, Map<Vector2i,Node> gridMap, Vector2i[] neighbours, Random rand)
	{
		List<Node> byDepth = chart.stream().filter(n -> n.metadata.depth() == depth).toList();
		for(Node node : byDepth)
		{
			Vector2i position = new Vector2i(0,0);
			if(gridMap.isEmpty())
			{
				node.setPosition(0, 0);
				gridMap.put(node.position, node);
			}
			else if(!node.parentLinks.isEmpty())
			{
				// Find unoccupied position adjacent to parent(s)
				List<Node> parents = node.getParents(chart);
				if(!parents.isEmpty())
				{
					List<Vector2i> options = Lists.newArrayList();
					for(Node parent : parents)
						for(Vector2i offset : neighbours)
						{
							Vector2i neighbour = add(parent.position, offset);
							if(gridMap.keySet().stream().noneMatch(neighbour::equals))
								options.add(neighbour);
						}
					
					if(options.isEmpty())
						continue;	// FIXME
					position = options.size() == 1 ? options.get(0) : options.get(rand.nextInt(options.size()));
				}
				
				node.setPosition(position.x, position.y);
				gridMap.put(position, node);
			}
		}
	}
	
	public static Vector2i add(Vector2i a, Vector2i b)
	{
		return new Vector2i(a.x + b.x, a.y + b.y);
	}
	
	private class Node
	{
		private final UUID id;
		private final CDMetadata metadata;
		private List<UUID> childLinks = Lists.newArrayList();
		private List<UUID> parentLinks = Lists.newArrayList();
		private Vector2i position = new Vector2i(0,0);
		
		public Node(UUID idIn, CDMetadata termIn, List<UUID> childLinksIn, List<UUID> parentLinksIn)
		{
			id = idIn;
			metadata = termIn;
			childLinks.addAll(childLinksIn);
			parentLinks.addAll(parentLinksIn);
		}
		
		public Node setPosition(int x, int y)
		{
			position = new Vector2i(x, y);
			return this;
		}
		
		public List<Node> getParents(List<Node> graph)
		{
			return graph.stream().filter(n -> parentLinks.contains(n.id)).toList();
		}
		
		public void render(DrawContext context, Vector2i origin, List<Node> chart, int mouseX, int mouseY)
		{
			// Render links first
			renderRecursive(origin, context, chart, mouseX, mouseY, false);
			// Then render icons & titles
			renderRecursive(origin, context, chart, mouseX, mouseY, true);
		}
		
		public void renderRecursive(Vector2i origin, DrawContext context, List<Node> chart, int mouseX, int mouseY, boolean drawIcon)
		{
			Vector2i pos = add(position, origin);
			// Render node links
			if(!childLinks.isEmpty())
				chart.stream().filter(n -> childLinks.contains(n.id)).forEach(child -> 
				{
					// Render link to child
					if(!drawIcon)
						renderLink(pos, add(child.position, origin), context);
					
					child.renderRecursive(origin, context, chart, mouseX, mouseY, drawIcon);
				});
			
			// Render node
			if(drawIcon)
			{
				context.drawTexture(RenderLayer::getGuiTextured, ICON_TEX, pos.x - 8, pos.y - 8, 0F, 0F, 16, 16, 16, 16, ColorHelper.withAlpha(255, metadata.type().colour()));
				
				if(pos.distance(mouseX, mouseY) < 30)
				{
					Text title = metadata.name();
					context.drawText(textRenderer, title, pos.x - (textRenderer.getWidth(title) / 2), pos.y - (textRenderer.fontHeight / 2), 0xFFFFFF, true);
				}
			}
		}
		
		private static void renderLink(Vector2i start, Vector2i end, DrawContext context)
		{
			final int linkColor = 0x6E6E6E;
			final int thickness = 1;
			
			renderStraightLine(start, end, thickness, context, linkColor);
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
		
//		private static void renderJointedLine(Vector2i start, Vector2i end, DrawContext context, int border, int rgba)
//		{
//			Vector2i joint = new Vector2i(end.x, start.y);
//			int thickness = 1;
//			// Draw line from start to joint
//			renderLine(start, joint, context, thickness, rgba);
//			// Draw line from joint to end
//			renderLine(joint, end, context, thickness, rgba);
//		}
//		
//		private static void renderLine(Vector2i a, Vector2i b, DrawContext context, int border, int rgba)
//		{
//			int minX = Math.min(a.x, b.x) - border;
//			int maxX = Math.max(a.x, b.x) + border;
//			int minY = Math.min(a.y, b.y) - border;
//			int maxY = Math.max(a.y, b.y) + border;
//			context.fill(minX, minY, maxX, maxY, rgba);
//		}
	}
}
