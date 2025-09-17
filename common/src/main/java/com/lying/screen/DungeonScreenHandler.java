package com.lying.screen;

import java.util.Optional;

import com.lying.grammar.GrammarPhrase;
import com.lying.init.CDScreenHandlerTypes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class DungeonScreenHandler extends ScreenHandler
{
	private Optional<GrammarPhrase> displayedGraph = Optional.empty();
	
	public DungeonScreenHandler(int syncId)
	{
		super(CDScreenHandlerTypes.DUNGEON_LAYOUT_HANDLER.get(), syncId);
	}
	
	public boolean canUse(PlayerEntity player) { return true; }
	public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
	
	public Optional<GrammarPhrase> graph() { return displayedGraph; }
	
	public void setDisplayedGraph(GrammarPhrase graphIn) { displayedGraph = Optional.of(graphIn); }
}
