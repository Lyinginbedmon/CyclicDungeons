package com.lying.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.Blueprint.ErrorType;
import com.lying.blueprint.BlueprintOrganiser;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.BlueprintScruncher;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;
import com.lying.utility.Box2f;
import com.lying.utility.LineSegment2f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;

public class DungeonScreen extends HandledScreen<DungeonScreenHandler>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	public static final Identifier ICON_TEX = Reference.ModInfo.prefix("textures/gui/tree_node.png");
	
	private State state = State.BLUEPRINT;
	
	public static int renderScale = 1;
	static boolean showCriticalPath = false;
	public static List<BlueprintPassage> criticalPath = Lists.newArrayList();
	public static List<BlueprintPassage> totalPassages = Lists.newArrayList();
	
	private final Long randSeed;
	private Vector2i displayOffset = new Vector2i(0,0);
	private Vector2i dragStart = null;
	
	private ButtonWidget scrunchButton, collapseButton, criticalButton;
	private ButtonWidget[] blueprintButtons;
	
	private Blueprint blueprint = null;
	private Map<ErrorType,Integer> errorCache = new HashMap<>();
	
	private Box2f originBox = null;
	private List<LineSegment2f> originLines = Lists.newArrayList();
	
	public DungeonScreen(DungeonScreenHandler handler, PlayerInventory inventory, Text title)
	{
		super(handler, inventory, title);
		randSeed = System.currentTimeMillis();
		renderScale = 16;
		showCriticalPath = false;
	}
	
	protected void init()
	{
		ButtonWidget tree, x4, x8, circle;
		addDrawableChild(tree = ButtonWidget.builder(Text.literal("Tree"), b -> organise(BlueprintOrganiser.Tree::create)).dimensions(0, 0, 60, 20).build());
		addDrawableChild(x4 = ButtonWidget.builder(Text.literal("4x Grid"), b -> organise(BlueprintOrganiser.Grid.Square::create)).dimensions(0, 20, 60, 20).build());
		addDrawableChild(x8 = ButtonWidget.builder(Text.literal("8x Grid"), b -> organise(BlueprintOrganiser.Grid.Octagonal::create)).dimensions(0, 40, 60, 20).build());
		addDrawableChild(circle = ButtonWidget.builder(Text.literal("Concentric"), b -> organise(BlueprintOrganiser.Circular::create)).dimensions(0, 60, 60, 20).build());
		
		addDrawableChild(scrunchButton = ButtonWidget.builder(Text.literal("Scrunch"), b -> scrunch()).dimensions(0, 90, 60, 20).build());
		addDrawableChild(collapseButton = ButtonWidget.builder(Text.literal("Collapse"), b -> collapse()).dimensions(0, 110, 60, 20).build());
		
		ButtonWidget goStart;
		addDrawableChild(goStart = ButtonWidget.builder(Text.literal("O"), b -> resetDrag())
				.tooltip(Tooltip.of(Text.literal("Focus Start")))
				.dimensions(width - 20, 0, 20, 20).build());
		addDrawableChild(criticalButton = ButtonWidget.builder(Text.literal("X"), b -> showCriticalPath = !showCriticalPath)
				.tooltip(Tooltip.of(Text.literal("Toggle Critical Path")))
				.dimensions(width - 20, 25, 20, 20).build());
		
		blueprintButtons = new ButtonWidget[] 
				{
					tree, x4, x8, circle, scrunchButton, collapseButton, goStart, criticalButton
				};
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Mode"), b -> state = State.values()[(state.ordinal() + 1)%State.values().length]).dimensions(width - 60, height - 20, 60, 20).build());
	}
	
	private void organise(Supplier<BlueprintOrganiser> organiser)
	{
		organiser.get().organise(blueprint, Random.create(randSeed));
		cacheErrors();
		updatePathCaches();
		resetDrag();
	}
	
	private void scrunch()
	{
		BlueprintScruncher.scrunch(blueprint, false);
		BlueprintScruncher.scrunch(blueprint, true);
		cacheErrors();
		updatePathCaches();
	}
	
	private void collapse()
	{
		BlueprintScruncher.collapse(blueprint, false);
		BlueprintScruncher.collapse(blueprint, true);
		cacheErrors();
		updatePathCaches();
	}
	
	private void cacheErrors()
	{
		errorCache.clear();
		for(ErrorType type : ErrorType.values())
		{
			int total = Blueprint.tallyErrors(blueprint, type);
			if(total > 0)
				errorCache.put(type, total);
		}
	}
	
	private void updatePathCaches()
	{
		totalPassages = BlueprintOrganiser.getFinalisedPassages(blueprint);
		
		if(!errorCache.isEmpty())
		{
			criticalPath.clear();
			return;
		}
		
		criticalPath = BlueprintOrganiser.getPassages(blueprint.getCriticalPath());
	}
	
	protected void drawForeground(DrawContext context, int mouseX, int mouseY)
	{
		switch(state)
		{
			case BLUEPRINT:
				context.drawText(textRenderer, this.title, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(this.title)) / 2, 10, 0xFFFFFF, false);
				
				if(blueprint != null && !blueprint.isEmpty())
				{
					int totalErrors = 0;
					for(int val : errorCache.values())
						totalErrors += val;
					boolean errors = totalErrors > 0;
					
					MutableText details = errors ? 
							Text.translatable("gui.cydun.dungeon_data_long", 
								blueprint.size(), 
								blueprint.maxDepth(), 
								totalErrors, 
								errorCache.getOrDefault(ErrorType.COLLISION, 0), 
								errorCache.getOrDefault(ErrorType.INTERSECTION, 0), 
								errorCache.getOrDefault(ErrorType.TUNNEL, 0)) :
							Text.translatable("gui.cydun.dungeon_data", 
								blueprint.size(), 
								blueprint.maxDepth(), 0);
					context.drawText(textRenderer, details, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(details)) / 2, 20, 0xFFFFFF, false);
					
					if(errors)
					{
						int y = mc.getWindow().getScaledHeight();
						for(Text line : new Text[] 
								{
									Text.literal("Dark Gray - Errors detected elsewhere"),
									Text.literal("Lime Green - Path intersects with unrelated room"),
									Text.literal("Light Blue - Path intersects with unrelated path")
								})
						{
							y -= textRenderer.fontHeight;
							context.drawText(textRenderer, line, 10, y, Formatting.GRAY.getColorValue(), false);
						}
					}
				}
				break;
			case DEBUG:
				context.drawText(textRenderer, Text.literal("Geometry testing"), (mc.getWindow().getScaledWidth() - textRenderer.getWidth(this.title)) / 2, 10, 0xFFFFFF, false);
				break;
		}
		
	}
	
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY)
	{
		blur();
		
		switch(state)
		{
			case BLUEPRINT:
				if(blueprint == null || blueprint.isEmpty())
					return;
				
				final Vector2i position = isDragging() ? new Vector2i(displayOffset.x + mouseX - dragStart.x, displayOffset.y + mouseY - dragStart.y) : displayOffset;
				final Vector2i drawOrigin = new Vector2i(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 5).add(position);
				NodeRenderUtils.render(blueprint.get(0), context, this.textRenderer, drawOrigin, blueprint, errorCache, mouseX, mouseY, renderScale);
				break;
			case DEBUG:
				Vec2f lineStart = new Vec2f(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 5);
				Vec2f lineEnd = new Vec2f(mouseX, mouseY);
				LineSegment2f line = new LineSegment2f(lineStart, lineEnd);
				NodeRenderUtils.renderGradientStraightLine(line, context, ColorHelper.withAlpha(175, Formatting.AQUA.getColorValue()));
				Vec2f textPoint = lineStart.add(lineEnd.add(lineStart.negate()).multiply(0.5F));
				
				// Test boundary box
				List<Pair<LineSegment2f,Vec2f>> intercepts = Lists.newArrayList();
				for(LineSegment2f edge : originLines)
				{
					Vec2f intercept = LineSegment2f.segmentIntercept(edge, line);
					boolean hasIntercept = intercept != null;
					int colour = hasIntercept ? NodeRenderUtils.GOLD : NodeRenderUtils.DARK_GRAY;
					NodeRenderUtils.renderGradientStraightLine(edge, context, colour);
					
					if(hasIntercept)
						intercepts.add(new Pair<LineSegment2f,Vec2f>(edge, intercept));
				}
				
				intercepts.forEach(p -> 
				{
					LineSegment2f edge = p.getLeft();
					Vec2f intercept = p.getRight();
					
					Box2f pointBox = new Box2f(intercept.x - 1, intercept.x + 1, intercept.y - 1, intercept.y + 1);
					NodeRenderUtils.renderBox(pointBox, context, ColorHelper.withAlpha(255, Formatting.DARK_RED.getColorValue()));
					
					context.drawText(
							textRenderer, 
							Text.literal(edge.asEquation()), 
							(int)(edge.isVertical ? edge.getLeft().x + 5 : (edge.getLeft().x + edge.getRight().x) / 2), 
							(int)(edge.isVertical ? (edge.getLeft().y + edge.getRight().y) / 2 : edge.getLeft().y + 5), 
							Formatting.DARK_RED.getColorValue(), 
							false);
				});
				
				context.drawText(textRenderer, Text.literal(line.asEquation()), (int)textPoint.x, (int)textPoint.y, 0xFFFFFF, false);
				break;
		}
	}
	
	public void handledScreenTick()
	{
		switch(state)
		{
			case BLUEPRINT:
				for(ButtonWidget button : blueprintButtons)
					button.visible = true;
				
				if(blueprint == null)
					getScreenHandler().graph().ifPresent(graph -> 
					{
						if(graph.isEmpty())
							return;
						
						blueprint = Blueprint.fromGraph(graph);
						blueprint.updateCriticalPath();
						Random rand = Random.create(randSeed);
						blueprint.stream().map(BlueprintRoom::metadata).forEach(meta -> meta.type().prepare(meta, rand));
						BlueprintOrganiser.Tree.create().organise(blueprint, rand);
						cacheErrors();
						updatePathCaches();
					});
				else
				{
					scrunchButton.active = collapseButton.active = criticalButton.active = errorCache.isEmpty();
					showCriticalPath = showCriticalPath && criticalButton.active;
				}
				break;
			case DEBUG:
				for(ButtonWidget button : blueprintButtons)
					button.visible = false;
				
				if(originBox == null)
				{
					final Vector2i screenMid = new Vector2i(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 2);
					originBox = new Box2f(screenMid.x - 50, screenMid.x + 50, screenMid.y - 50, screenMid.y + 50);
					originLines.clear();
					originLines.addAll(originBox.asEdges());
				}
				break;
		}
	}
	
	public void resetDrag() { blueprint.start().ifPresent(s -> displayOffset = s.position()); }
	
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
		int prevScale = renderScale;
		renderScale = MathHelper.clamp(renderScale + (int)verticalAmount, 1, 500);
		
		float delta = (float)renderScale / (float)prevScale;
		float offX = this.displayOffset.x * delta;
		float offY = this.displayOffset.y * delta;
		this.displayOffset = new Vector2i((int)offX, (int)offY);
		
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	
	public static enum State
	{
		BLUEPRINT,
		DEBUG;
	}
}
