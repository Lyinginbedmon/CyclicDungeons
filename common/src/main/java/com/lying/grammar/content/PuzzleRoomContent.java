package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.init.CDContentTypes.ContentEntry;
import com.lying.worldgen.theme.Theme;

import net.minecraft.util.Identifier;

public class PuzzleRoomContent extends RegistryRoomContent<ContentEntry>
{
	public static final Identifier ID	= prefix("puzzle");
	
	public PuzzleRoomContent()
	{
		super(ID);
	}
	
	public void buildRegistry(Theme theme)
	{
		theme.puzzles().forEach(puzzle -> register(puzzle.registryName(), puzzle));
	}
}
