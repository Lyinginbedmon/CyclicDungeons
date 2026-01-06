package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.worldgen.theme.Theme;

import net.minecraft.util.Identifier;

public class TrapRoomContent extends RegistryRoomContent<TrapEntry>
{
	public static final Identifier ID	= prefix("trap");
	
	public TrapRoomContent()
	{
		super(ID);
	}
	
	public void buildRegistry(Theme theme)
	{
		theme.traps().forEach(trap -> register(trap.registryName(), trap));
	}
	
	public static abstract class TrapEntry implements IContentEntry
	{
		private final Identifier id;
		
		protected TrapEntry(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
	}
}
