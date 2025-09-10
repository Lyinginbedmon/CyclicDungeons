package com.lying.client.screen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.grammar.CDGraph;
import com.lying.grammar.CDMetadata;
import com.lying.grammar.CDRoom;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;
import com.lying.utility.Box2;
import com.lying.utility.Line2;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
	private Vector2i displayOffset = new Vector2i(0,0);
	private Vector2i dragStart = null;
	
	private List<Node> chart = null;
	public static final int RENDER_SCALE = 16;
	
	private static final Identifier ICON_TEX = Reference.ModInfo.prefix("textures/gui/tree_node.png");
	
	public DungeonScreen(DungeonScreenHandler handler, PlayerInventory inventory, Text title)
	{
		super(handler, inventory, title);
	}
	
	protected void init()
	{
		addDrawableChild(ButtonWidget.builder(Text.literal("Crunch"), b -> crunchGraph()).dimensions(0, 0, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Collapse"), b -> collapseGraph()).dimensions(0, 20, 60, 20).build());
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
		if(chart != null && !chart.isEmpty())
			chart.get(0).render(context, origin, chart, mouseX, mouseY, RENDER_SCALE);
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
				Random rand = new Random();
//				organisePositionsByRow(chart);
				organisePositionsByGrid(chart);
				
				chart.forEach(node -> 
				{
					int x = 2 + rand.nextInt(4);
					int y = 2 + rand.nextInt(4);
					node.metadata.setSize(x, y);
				});
			});
		}
	}
	
	/** Applies crunch algorithm until failure */
	public void collapseGraph()
	{
		while(crunchGraph()) { }
	}
	
	/** Reduces the distance between nodes */
	public boolean crunchGraph()
	{
		int maxDepth = 0;
		for(Node node : chart)
			if(node.metadata.depth() > maxDepth)
				maxDepth = node.metadata.depth();
		
		boolean anyMoved = false;
		for(int i=maxDepth; i>0; i--)
		{
			final int depth = i;
			Map<Node, List<Node>> nodesAtDepth = new HashMap<>();
			chart.stream()
				.filter(n -> n.metadata.depth() == depth)
				.forEach(n -> nodesAtDepth.put(n, n.getParents(chart)));
			
			anyMoved = tryCrunch(nodesAtDepth) || anyMoved;
		}
		return anyMoved;
	}
	
	private boolean tryCrunch(Map<Node, List<Node>> nodes)
	{
		boolean anyMoved = false;
		for(Entry<Node, List<Node>> entry : nodes.entrySet())
		{
			// Calculate "ideal" position, ie. right on top of or between the parents
			int x = 0, y = 0;
			if(entry.getValue().size() > 1)
			{
				for(Node parent : entry.getValue())
				{
					x += parent.position.x;
					y += parent.position.y;
				}
				x /= entry.getValue().size();
				y /= entry.getValue().size();
			}
			else
			{
				Node parent = entry.getValue().get(0);
				x = parent.position.x;
				y = parent.position.y;
			}
			Vector2i ideal = new Vector2i(x, y);
			
			Node node = entry.getKey();
			
			// Amount & direction to move
			double len = subtract(node.position, ideal).length();
			if(len < 1)
				continue;
			
			// Collect node and all descendants as a "cluster"
			List<Node> toMove = gatherDescendantsOf(node, chart);
			toMove.add(node);
			
			List<Node> otherNodes = chart.stream().filter(n -> !toMove.contains(n)).toList();
			while(len-- > 0 && tryMoveTowards(node, toMove, otherNodes, ideal))
				anyMoved = true;
		};
		return anyMoved;
	}
	
	public static List<Node> gatherDescendantsOf(Node node, List<Node> chart)
	{
		List<Node> children = Lists.newArrayList();
		node.getChildren(chart).forEach(child -> 
		{
			if(children.contains(child))
				return;
			
			children.add(child);
			if(child.hasChildren())
				children.addAll(gatherDescendantsOf(child, chart));
		});
		// FIXME Ensure uniqueness in list of children
		return children;
	}
	
	private boolean tryMoveTowards(Node node, List<Node> cluster, List<Node> otherNodes, Vector2i point)
	{
		// Current position, from which we calculate offset
		Vector2i position = node.position;
		
		Vector2i offset = subtract(position, point);
		double len = offset.length();
		if(len < 1)
			return false;
		
		offset = new Vector2i(
				offset.x != 0 ? (int)Math.signum(offset.x) : 0, 
				offset.y != 0 ? (int)Math.signum(offset.y) : 0);
		
		// Try move the full offset, then if that fails try to move on one axis
		if(tryMove(cluster, otherNodes, offset))
			return true;
		else
		{
			// If both axises are non-zero, try to move on each individually
			if(offset.x != 0 && offset.y != 0)
			{
				Vector2i onlyX = new Vector2i(offset.x, 0);
				Vector2i onlyY = new Vector2i(0, offset.y);
				double distX = add(position, onlyX).distance(point);
				double distY = add(position, onlyY).distance(point);
				
				Supplier<Boolean> tryX = () -> tryMove(cluster, otherNodes, onlyX);
				Supplier<Boolean> tryY = () -> tryMove(cluster, otherNodes, onlyY);
				
				// Prioritise moving in whichever direction results in the shortest distance to the point
				if(distX < distY)
					return tryX.get() ? true : tryY.get();
				else
					return tryY.get() ? true : tryX.get();
			}
			else
				return false;
		}
	}
	
	private boolean tryMove(List<Node> cluster, List<Node> otherNodes, Vector2i move)
	{
		if(move.length() == 0 || cluster.isEmpty())
			return false;
		
		for(Node n : cluster)
		{
			Box2 bounds = n.bounds().offset(move);
			if(otherNodes.stream().anyMatch(o -> o.intersects(bounds)))
				return false;
		}
		
		for(Node n : cluster)
			n.offset(move);
		
		return true;
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
			this.displayOffset = this.displayOffset.add(xOff, yOff);
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
		
		final GridPosition[] moveSet = new GridPosition[]
				{
					(p,o,g) -> add(p, new Vector2i(g, 0)),
					(p,o,g) -> add(p, new Vector2i(-g, 0)),
					(p,o,g) -> add(p, new Vector2i(0, g)),
					(p,o,g) -> add(p, new Vector2i(0, -g)),
					
					(p,o,g) -> add(p, new Vector2i(g, g)),
					(p,o,g) -> add(p, new Vector2i(g, -g)),
					(p,o,g) -> add(p, new Vector2i(-g, g)),
					(p,o,g) -> add(p, new Vector2i(-g, -g))
				};
		
		int maxDepth = 0;
		for(Node node : chart)
			if(node.metadata.depth() > maxDepth)
				maxDepth = node.metadata.depth();
		
		Random rand = new Random();
		for(int step = 0; step <= maxDepth; step++)
			organiseByGrid(chart, step, gridMap, moveSet, 10, rand);
		
		if(gridMap.size() != chart.size())
			CyclicDungeons.LOGGER.warn("Grid layout size ({}) differs from input graph size ({})", gridMap.size(), chart.size());
	}
	
	private static void organiseByGrid(List<Node> chart, int depth, Map<Vector2i,Node> gridMap, GridPosition[] moveSet, int gridSize, Random rand)
	{
		List<Node> byDepth = chart.stream().filter(n -> n.metadata.depth() == depth).toList();
		for(Node node : byDepth)
			if(gridMap.isEmpty())
			{
				node.setPosition(0, 0);
				gridMap.put(node.position, node);
			}
			else if(!node.parentLinks.isEmpty())
			{
				Vector2i position = new Vector2i(0,0);
				
				// Find unoccupied position adjacent to parent(s)
				List<Node> parents = node.getParents(chart);
				if(!parents.isEmpty())
				{
					List<Vector2i> options = getAvailableOptions(parents, node.childLinks.size(), moveSet, gridSize, gridMap);
					if(options.isEmpty())
						continue;	// FIXME Resolve contexts where nodes have nowhere to go
					
					// FIXME Prioritise positions to reduce "snarling"
					position = 
							options.size() == 1 ? 
								options.get(0) : 
								options.get(rand.nextInt(options.size()));
				}
				
				node.setPosition(position.x, position.y);
				gridMap.put(position, node);
			}
	}
	
	public static List<Vector2i> getAvailableOptions(List<Node> parents, int childTally, GridPosition[] moveSet, int gridSize, Map<Vector2i,Node> gridMap)
	{
		List<Vector2i> options = Lists.newArrayList();
		for(Node parent : parents)
			getAvailableOptions(parent.position, childTally, moveSet, gridSize, gridMap).stream().filter(p -> !options.contains(p)).forEach(options::add);
		
		// Make list of all existing paths in gridMap
		List<Line2> existingPaths = getPaths(gridMap.values());
		
		if(!existingPaths.isEmpty())
			options.removeIf(pos -> 
			{
				for(Node parent : parents)
				{
					// Test if the path intersects with any existing path in the grid
					final Line2 a = new Line2(pos, parent.position);
					if(existingPaths.stream().anyMatch(p -> a.intersects(p)))
						return true;
				}
				
				return false;
			});
		
		return options;
	}
	
	/** Returns a list of all paths between the given nodes */
	public static List<Line2> getPaths(Collection<Node> chart)
	{
		List<Line2> existingPaths = Lists.newArrayList();
		for(Node node : chart)
			for(UUID childId : node.childLinks)
				chart.stream().filter(c -> c.id.equals(childId)).findAny().ifPresent(c -> existingPaths.add(new Line2(node.position, c.position)));
		return existingPaths;
	}
	
	public static List<Line2> getPaths(Node node, List<Node> chart)
	{
		List<Line2> existingPaths = Lists.newArrayList();
		for(UUID childId : node.childLinks)
			chart.stream().filter(c -> c.id.equals(childId)).findAny().ifPresent(c -> existingPaths.add(new Line2(node.position, c.position)));
		return existingPaths;
	}
	
	public static List<Vector2i> getAvailableOptions(Vector2i position, int minExits, GridPosition[] moveSet, int gridSize, Map<Vector2i,Node> gridMap)
	{
		List<Vector2i> options = Lists.newArrayList();
		for(GridPosition offset : moveSet)
		{
			Vector2i neighbour = offset.get(position, gridMap, gridSize);
			if(gridMap.keySet().stream().noneMatch(neighbour::equals))
			{
				// Ensure the position has at least as many moves itself as the node has children
				if(minExits > 0 && getAvailableOptions(neighbour, -1, moveSet, gridSize, gridMap).size() < minExits)
					continue;
				
				options.add(neighbour);
			}
		}
		return options;
	}
	
	@FunctionalInterface
	public interface GridPosition
	{
		public Vector2i get(Vector2i position, Map<Vector2i,Node> occupancies, int gridSize);
	}
	
	public static Vector2i add(Vector2i a, Vector2i b)
	{
		return new Vector2i(a.x + b.x, a.y + b.y);
	}
	
	public static Vector2i subtract(Vector2i val, Vector2i from)
	{
		return new Vector2i(from.x - val.x, from.y - val.y);
	}
	
	public static Vector2i mul(Vector2i a, int scalar)
	{
		return new Vector2i(a.x * scalar, a.y * scalar);
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
		
		public Node offset(Vector2i vec)
		{
			return offset(vec.x, vec.y);
		}
		
		public Node offset(int x, int y)
		{
			position = position.add(x, y);
			return this;
		}
		
		public boolean hasChildren() { return !childLinks.isEmpty(); }
		
		public List<Node> getParents(List<Node> graph)
		{
			return graph.stream().filter(n -> parentLinks.contains(n.id)).toList();
		}
		
		public List<Node> getChildren(List<Node> graph)
		{
			List<Node> set = Lists.newArrayList();
			set.addAll(graph.stream().filter(n -> childLinks.contains(n.id)).toList());
			return set;
		}
		
		public Box2 bounds()
		{
			int sizeX = metadata.size().x;
			int sizeY = metadata.size().y;
			int minX = position.x - (sizeX / 2);
			int minY = position.y - (sizeY / 2);
			return new Box2(minX, minX + sizeX, minY, minY + sizeY);
		}
		
		public boolean intersects(Box2 boundsB)
		{
			Box2 bounds = bounds();
			return bounds.intersects(boundsB) || boundsB.intersects(bounds);
		}
		
		public void render(DrawContext context, Vector2i origin, List<Node> chart, int mouseX, int mouseY, int renderScale)
		{
			// Render links first
			renderRecursive(origin, context, chart, mouseX, mouseY, renderScale, false);
			// Then render icons & titles
			renderRecursive(origin, context, chart, mouseX, mouseY, renderScale, true);
		}
		
		public void renderRecursive(Vector2i origin, DrawContext context, List<Node> chart, int mouseX, int mouseY, int renderScale, boolean drawIcon)
		{
			Vector2i pos = add(mul(position, renderScale), origin);
			int colour = ColorHelper.withAlpha(255, metadata.type().colour());
			if(drawIcon)
			{
				Vector2i size = metadata.size();
				context.drawBorder(pos.x - (size.x / 2) * renderScale, pos.y - (size.y / 2) * renderScale, size.x * renderScale, size.y * renderScale, colour);
			}
			
			// Render node links
			if(!childLinks.isEmpty())
				chart.stream().filter(n -> childLinks.contains(n.id)).forEach(child -> 
				{
					// Render link to child
					if(!drawIcon)
						renderLink(pos, add(mul(child.position, renderScale), origin), context);
					
					child.renderRecursive(origin, context, chart, mouseX, mouseY, renderScale, drawIcon);
				});
			
			// Render node
			if(drawIcon)
			{
				context.drawTexture(RenderLayer::getGuiTextured, ICON_TEX, pos.x - 8, pos.y - 8, 0F, 0F, 16, 16, 16, 16, colour);
				
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
	}
}
