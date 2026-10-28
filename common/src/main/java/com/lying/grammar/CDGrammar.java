package com.lying.grammar;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.lying.init.CDLoggers;
import com.lying.init.CDTerms;
import com.lying.init.CDThemes;
import com.lying.utility.CDUtils;
import com.lying.utility.logging.DebugLogger;

import net.minecraft.util.math.random.Random;

public class CDGrammar
{
	private static final DebugLogger LOGGER = CDLoggers.GRAMMAR;
	private static List<GrammarTerm> PLACEABLES = List.of();
	
	/** Generates a relatively linear initial starting graph */
	public static GrammarPhrase initialPhrase(int blanks, Random rand)
	{
		GrammarPhrase graph = new GrammarPhrase();
		GrammarRoom start = new GrammarRoom();
		start.metadata().setType(CDTerms.instance().start());
		graph.add(start);
		
		GrammarRoom previous = start;
		for(int i=0; i<blanks; i++)
		{
			GrammarRoom blank = new GrammarRoom();
			previous.linkTo(blank);
			
			graph.add(blank);
			if(previous.canAddLink() && rand.nextBoolean())
				;
			else
				previous = blank;
		}
		
		GrammarRoom end = new GrammarRoom();
		end.metadata().setType(CDTerms.instance().end());
		previous.linkTo(end);
		graph.add(end);
		return graph;
	}
	
	/** Populates the given graph */
	public static GrammarPhrase generate(GrammarPhrase graph, Random rand)
	{
		if(graph == null || graph.isEmpty())
			throw new UnsupportedOperationException("Cannot apply grammar generation to a null or empty phrase");
		
		PLACEABLES = CDThemes.instance().get(graph.theme()).get().getPlaceableTerms();
		final Optional<GrammarRoom> start = graph.getStart();
		if(start.isPresent())
		{
			int iterationCap = 500;
			while(graph.hasBlanks() && iterationCap-- > 0)
				recursiveGenerate(start.get(), graph, rand);
		}
		
		if(graph.hasBlanks())
			LOGGER.error("Ran out of iterations before all blanks were replaced in phrase");
		
		return graph;
	}
	
	private static void recursiveGenerate(GrammarRoom room, GrammarPhrase graph, Random rand)
	{
		if(room.metadata().isReplaceable())
			generate(room, graph, rand);
		room.getChildRooms(graph).forEach(r -> recursiveGenerate(r, graph, rand));
	}
	
	private static void generate(GrammarRoom room, GrammarPhrase graph, Random rand)
	{
		
		/** Rooms that connect to this one */
		final List<GrammarRoom> previous = room.getParentRoom(graph);
		
		/** Rooms immediately following this one */
		final List<GrammarRoom> next = room.getChildRooms(graph);
		
		GrammarTerm term = null;
		final List<GrammarTerm> candidates = PLACEABLES.stream().filter(t -> t.canBePlaced(room, previous, next, graph)).toList();
		switch(candidates.size())
		{
			case 0:
				LOGGER.warn("No viable terms found to replace {}", room.name().getString());
				term = CDTerms.VOID;
				break;
			case 1:
				term = candidates.get(0);
				break;
			default:
				term = CDUtils.selectFromWeightedList(candidates.stream().map(c -> Pair.of(c, c.weight())).toList(), rand);
				break;
		}
		
		room.applyTerm(term, graph);
	}
}
