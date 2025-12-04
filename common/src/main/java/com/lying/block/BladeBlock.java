package com.lying.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;

public class BladeBlock extends Block
{
	public static final EnumProperty<Part> PART	= EnumProperty.of("part", Part.class, Part.MOUNT, Part.ARM, Part.BLADE);
	
	public BladeBlock(Settings settings)
	{
		super(settings.nonOpaque());
		setDefaultState(getDefaultState().with(PART, Part.MOUNT));
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(PART);
	}
	
	public static enum Part implements StringIdentifiable
	{
		MOUNT,
		ARM,
		BLADE;
		
		public String asString() { return name().toLowerCase(); }
	}
}
