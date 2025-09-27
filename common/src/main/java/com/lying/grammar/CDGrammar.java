package com.lying.grammar;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.lying.reference.Reference;
import com.lying.utility.CDUtils;

import net.minecraft.util.math.random.Random;

public class CDGrammar
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Reference.ModInfo.MOD_ID+"_grammar");
	
	public static final List<GrammarTerm> PLACEABLE = CDTerms.placeables();
	
	/** Generates a relatively linear initial starting graph */
	public static GrammarPhrase initialGraph(int blanks)
	{
		return initialGraph(blanks, Random.create());
	}
	
	/** Generates a relatively linear initial starting graph */
	public static GrammarPhrase initialGraph(int blanks, Random rand)
	{
		GrammarPhrase graph = new GrammarPhrase();
		GrammarRoom start = new GrammarRoom();
		start.metadata().setType(CDTerms.START.get());
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
		end.metadata().setType(CDTerms.END.get());
		previous.linkTo(end);
		graph.add(end);
		return graph;
	}
	
	/** Populates the given graph */
	public static GrammarPhrase generate(GrammarPhrase graph)
	{
		return generate(graph, Random.create());
	}
	
	/** Populates the given graph */
	public static GrammarPhrase generate(GrammarPhrase graph, Random rand)
	{
		int iterationCap = 5;
		while(!graph.isEmpty() && graph.hasBlanks() && --iterationCap > 0)
			recursiveGenerate(graph.get(0).get(), graph, rand);
		LOGGER.info(" # Generated {}:{} graph", graph.size(), graph.depth());
		return graph;
	}
	
	private static void recursiveGenerate(GrammarRoom room, GrammarPhrase graph, Random rand)
	{
		generate(room, graph, rand);
		room.getChildRooms(graph).forEach(r -> recursiveGenerate(r, graph, rand));
	}
	
	private static void generate(GrammarRoom room, GrammarPhrase graph, Random rand)
	{
		if(!room.metadata().isReplaceable())
			return;
		
		/** Rooms that connect to this one */
		final List<GrammarRoom> previous = room.getParentRooms(graph);
		
		/** Rooms immediately following this one */
		final List<GrammarRoom> next = room.getChildRooms(graph);
		
		GrammarTerm term = CDTerms.VOID.get();
		List<GrammarTerm> candidates = PLACEABLE.stream().filter(t -> t.canBePlaced(room, previous, next, graph)).toList();
		if(!candidates.isEmpty())
		{
			if(candidates.size() == 1)
				term = candidates.get(0);
			else
			{
				// Run weight calculation and select term accordingly
				List<Pair<GrammarTerm,Float>> weights = Lists.newArrayList();
				for(GrammarTerm candidate : candidates)
					weights.add(Pair.of(candidate, (float)candidate.weight()));
				
				term = CDUtils.selectFromWeightedList(weights, rand.nextFloat());
			}
		}
		else
			LOGGER.warn("No candidates found to replace {}", room.name().getString());
		
		room.applyTerm(term, graph);
	}
}
