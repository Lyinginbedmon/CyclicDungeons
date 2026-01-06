package com.lying.grammar.modifier;

import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;

import net.minecraft.util.Identifier;

public class InjectBranch extends PhraseModifier
{
	public InjectBranch(Identifier id)
	{
		super(id);
	}
	
	public boolean isBranchInjector() { return true; }
	
	public void apply(GrammarTerm term, GrammarRoom room, GrammarPhrase graph)
	{
		GrammarRoom injected = new GrammarRoom();
		room.linkTo(injected);
		graph.add(injected);
	}
}