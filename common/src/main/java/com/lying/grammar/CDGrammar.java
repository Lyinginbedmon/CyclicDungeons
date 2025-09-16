package com.lying.grammar;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.init.CDTerms;
import com.lying.utility.CDUtils;

public class CDGrammar
{
	private static final Logger LOGGER = CyclicDungeons.LOGGER;
	
	public static final List<GrammarTerm> PLACEABLE = CDTerms.placeables();
	
	public static void run()
	{
		final int scale = 5;
		final Function<CDRoom,String> func = r -> "	".repeat(r.metadata().depth()) + r.asString();
		LOGGER.info("Initial graph:");
		CDGraph initial = initialGraph(scale);
		initial.printAsTree(LOGGER::info, func);
		for(int i=0; i<5; i++)
		{
			LOGGER.info("Generated graph {}:", i);
			generate(initial.clone()).printAsTree(LOGGER::info, func);
		}
	}
	
	/** Generates a linear initial starting graph */
	public static CDGraph initialGraph(int blanks)
	{
		Random rand = new Random();
		CDGraph graph = new CDGraph();
		CDRoom start = new CDRoom();
		start.metadata().setType(CDTerms.START.get());
		graph.add(start);
		
		CDRoom previous = start;
		for(int i=0; i<blanks; i++)
		{
			CDRoom blank = new CDRoom();
			previous.linkTo(blank);
			
			graph.add(blank);
			if(previous.canAddLink() && rand.nextBoolean())
				;
			else
				previous = blank;
		}
		
		CDRoom end = new CDRoom();
		end.metadata().setType(CDTerms.END.get());
		previous.linkTo(end);
		graph.add(end);
		return graph;
	}
	
	/** Populates the given graph */
	public static CDGraph generate(CDGraph graph)
	{
		Random rand = new Random();
		int iterationCap = 5;
		while(!graph.isEmpty() && graph.hasBlanks() && --iterationCap > 0)
			recursiveGenerate(graph.get(0).get(), graph, rand);
		return graph;
	}
	
	private static void recursiveGenerate(CDRoom room, CDGraph graph, Random rand)
	{
		generate(room, graph, rand);
		room.getChildRooms(graph).forEach(r -> recursiveGenerate(r, graph, rand));
	}
	
	private static void generate(CDRoom room, CDGraph graph, Random rand)
	{
		if(!room.metadata().isReplaceable())
			return;
		
		/** Rooms that connect to this one */
		final List<CDRoom> previous = room.getParentRooms(graph);
		
		/** Rooms immediately following this one */
		final List<CDRoom> next = room.getChildRooms(graph);
		
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
