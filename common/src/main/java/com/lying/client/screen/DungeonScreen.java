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
import com.lying.blueprint.BlueprintScruncher;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
	static boolean showCriticalPath = false;
	static List<BlueprintPassage> criticalPath = Lists.newArrayList();
	static List<BlueprintPassage> totalPassages = Lists.newArrayList();
	
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
		showCriticalPath = false;
	}
	
	protected void init()
	{
		addDrawableChild(ButtonWidget.builder(Text.literal("Tree"), b -> organise(BlueprintOrganiser.Tree::create)).dimensions(0, 0, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("4x Grid"), b -> organise(BlueprintOrganiser.Grid.Square::create)).dimensions(0, 20, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("8x Grid"), b -> organise(BlueprintOrganiser.Grid.Octagonal::create)).dimensions(0, 40, 60, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Concentric"), b -> organise(BlueprintOrganiser.Circular::create)).dimensions(0, 60, 60, 20).build());
		
		addDrawableChild(scrunchButton = ButtonWidget.builder(Text.literal("Scrunch"), b -> scrunch()).dimensions(0, 90, 60, 20).build());
		addDrawableChild(collapseButton = ButtonWidget.builder(Text.literal("Collapse"), b -> collapse()).dimensions(0, 110, 60, 20).build());
		
		addDrawableChild(routeButton = ButtonWidget.builder(Text.literal("X"), b -> showCriticalPath = !showCriticalPath).dimensions(width - 20, 0, 20, 20).build());
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
		totalPassages = BlueprintOrganiser.mergePassages(BlueprintOrganiser.getPassages(blueprint), BlueprintOrganiser.getBounds(blueprint));
		
		if(!errorCache.isEmpty())
		{
			criticalPath.clear();
			return;
		}
		
		criticalPath = BlueprintOrganiser.getPassages(blueprint.getCriticalPath());
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
						errorCache.getOrDefault(ErrorType.COLLISION, 0), 
						errorCache.getOrDefault(ErrorType.INTERSECTION, 0), 
						errorCache.getOrDefault(ErrorType.TUNNEL, 0)) :
					Text.translatable("gui.cydun.dungeon_data", 
						blueprint.size(), 
						blueprint.maxDepth(), 0);
			context.drawText(textRenderer, details, (mc.getWindow().getScaledWidth() - textRenderer.getWidth(details)) / 2, 20, 0xFFFFFF, false);
		}
	}
	
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY)
	{
		blur();
		
		if(blueprint != null && !blueprint.isEmpty())
		{
			final Vector2i position = isDragging() ? new Vector2i(displayOffset.x + mouseX - dragStart.x, displayOffset.y + mouseY - dragStart.y) : displayOffset;
			final Vector2i origin = new Vector2i(mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 5).add(position);
			NodeRenderUtils.render(blueprint.get(0), context, this.textRenderer, origin, blueprint, errorCache, mouseX, mouseY, renderScale);
		}
	}
	
	public void handledScreenTick()
	{
		if(blueprint == null)
			getScreenHandler().graph().ifPresent(graph -> 
			{
				if(graph.isEmpty())
					return;
				
				blueprint = Blueprint.fromGraph(graph);
				blueprint.updateCriticalPath();
				Random rand = Random.create(randSeed);
				blueprint.forEach(node -> node.metadata().setSize(node.metadata().type().size(rand)));
				BlueprintOrganiser.Tree.create().organise(blueprint, rand);
				cacheErrors();
				updatePathCaches();
			});
		else
		{
			scrunchButton.active = collapseButton.active = routeButton.active = errorCache.isEmpty();
			showCriticalPath = showCriticalPath && routeButton.active;
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
		int prevScale = renderScale;
		renderScale = MathHelper.clamp(renderScale + (int)verticalAmount, 1, 500);
		
		float delta = (float)renderScale / (float)prevScale;
		float offX = this.displayOffset.x * delta;
		float offY = this.displayOffset.y * delta;
		this.displayOffset = new Vector2i((int)offX, (int)offY);
		
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
