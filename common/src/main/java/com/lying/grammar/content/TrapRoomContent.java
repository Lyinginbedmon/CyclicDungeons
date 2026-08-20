package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.init.CDContentTypes.ContentEntry;
import com.lying.worldgen.theme.Theme;

import net.minecraft.util.Identifier;

public class TrapRoomContent extends RegistryRoomContent<ContentEntry>
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
}
