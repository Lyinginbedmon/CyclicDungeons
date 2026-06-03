package com.lying.client.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicCategory;
import com.lying.init.CDLogicGates.LogicGate;
import com.lying.reference.Reference;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CircuitScreen extends Screen
{
	private Map<LogicCategory, ButtonWidget> categoryButtons = new HashMap<>();
	private Map<LogicCategory, List<ButtonWidget>> categoryMap = new HashMap<>();
	private LogicCategory displayedCategory = null;
	
	public CircuitScreen()
	{
		super(Reference.ModInfo.translate("gui", "circuit_screen.title"));
	}
	
	protected void init()
	{
		super.init();
		categoryButtons.clear();
		categoryMap.clear();
		for(LogicCategory cat : LogicCategory.values())
		{
			int x = 5 + categoryButtons.size() * 60;
			ButtonWidget category = addDrawableChild(ButtonWidget.builder(Text.literal(cat.name()), b -> showCategory(cat)).dimensions(x, 5, 60, 20).build());
			categoryButtons.put(cat, category);
			
			List<ButtonWidget> buttons = Lists.newArrayList();
			for(LogicGate gate : CDLogicGates.byCategory(cat))
			{
				ButtonWidget button = addDrawableChild(ButtonWidget.builder(gate.displayName(), b -> {}).dimensions(x, 5 + 21 + buttons.size() * 20, 60, 20).build());
				button.visible = button.active = false;
				buttons.add(button);
			}
			categoryMap.put(cat, buttons);
		}
		showCategory(LogicCategory.BASIC);
	}
	
	public void showCategory(LogicCategory cat)
	{
		// Deactivate previous category
		if(displayedCategory != null)
		{
			categoryMap.get(displayedCategory).forEach(b -> b.visible = b.active = false);
			categoryButtons.get(displayedCategory).active = true;
		}
		
		// Activate new category
		displayedCategory = cat;
		categoryMap.get(displayedCategory).forEach(b -> b.visible = b.active = true);
		categoryButtons.get(displayedCategory).active = false;
	}
}
