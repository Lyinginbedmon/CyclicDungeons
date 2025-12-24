package com.lying.blueprint.processor;

import com.lying.blueprint.processor.TrapRoomProcessor.TrapEntry;
import com.lying.init.CDThemes.Theme;

import net.minecraft.util.Identifier;

public class TrapRoomProcessor extends RegistryRoomProcessor<TrapEntry>
{
	public void buildRegistry(Theme theme)
	{
		theme.traps().forEach(trap -> register(trap.registryName(), trap));
	}
	
	public static abstract class TrapEntry implements IProcessorEntry
	{
		private final Identifier id;
		
		protected TrapEntry(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
	}
}
