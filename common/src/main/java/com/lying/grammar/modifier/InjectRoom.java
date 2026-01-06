package com.lying.grammar.modifier;

import java.util.Optional;

import com.lying.grammar.DefaultTerms;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;

import net.minecraft.util.Identifier;

public class InjectRoom extends PhraseModifier
{
	public InjectRoom(Identifier id)
	{
		super(id);
	}
	
	public void apply(GrammarTerm term, GrammarRoom room, GrammarPhrase graph)
	{
		GrammarRoom injected = new GrammarRoom();
		setupRoom(injected, graph);
		
		room.getChildLinks().forEach(uuid -> 
		{
			Optional<GrammarRoom> child = graph.get(uuid);
			if(child.isEmpty())
				return;
			
			// Move all links of parent to child
			injected.linkTo(child.get());
			room.detachFrom(child.get());
		});
		
		// Link parent to child and add to graph
		room.linkTo(injected);
		graph.add(injected);
	}
	
	protected void setupRoom(GrammarRoom room, GrammarPhrase graph) { }
	
	public static class Treasure extends InjectRoom
	{
		public Treasure(Identifier id)
		{
			super(id);
		}
		
		protected void setupRoom(GrammarRoom room, GrammarPhrase graph)
		{
			room.applyTerm(DefaultTerms.TREASURE, graph);
		}
	}
}