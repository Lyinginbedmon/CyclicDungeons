package com.lying.grammar;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.slf4j.Logger;

import com.lying.CyclicDungeons;
import com.lying.init.CDTerms;

public class CDGrammar
{
	private static final Logger LOGGER = CyclicDungeons.LOGGER;
	
	public static final List<GrammarTerm> PLACEABLE = CDTerms.placeables();
	
	public static void run()
	{
		final int scale = 5;
		final Function<CDRoom,String> func = r -> "	".repeat(r.metadata().depth()) + r.asString();
		LOGGER.info("Initial graph:");
		initialGraph(scale).printAsTree(LOGGER::info, func);
		for(int i=0; i<5; i++)
		{
			LOGGER.info("Generated graph {}:", i);
			generate(initialGraph(scale)).printAsTree(LOGGER::info, func);
		}
	}
	
	/** Generates a linear initial starting graph */
	public static CDGraph initialGraph(int blanks)
	{
		CDGraph graph = new CDGraph();
		CDRoom start = new CDRoom();
		start.metadata().setType(CDTerms.START.get());
		graph.add(start);
		
		CDRoom previous = start;
		for(int i=0; i<blanks; i++)
		{
			CDRoom blank = new CDRoom();
			previous.linkTo(blank.uuid());
			
			previous = blank;
			graph.add(blank);
		}
		
		CDRoom end = new CDRoom();
		end.metadata().setType(CDTerms.END.get());
		previous.linkTo(end.uuid());
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
		room.getLinksFrom(graph).forEach(r -> recursiveGenerate(r, graph, rand));
	}
	
	private static void generate(CDRoom room, CDGraph graph, Random rand)
	{
		if(!room.metadata().isReplaceable())
			return;
		
		/** Rooms that connect to this one */	// TODO Store parent room IDs to reduce searching
		final List<CDRoom> previous = graph.getLinksTo(room.uuid());
		
		/** Rooms immediately following this one */
		final List<CDRoom> next = room.getLinksFrom(graph);
		
		GrammarTerm term = CDTerms.VOID.get();
		List<GrammarTerm> candidates = PLACEABLE.stream().filter(t -> t.canBePlaced(room, previous, next, graph)).toList();
		if(!candidates.isEmpty())
			term = candidates.size() == 1 ? candidates.get(0) : candidates.get(rand.nextInt(candidates.size()));
		else
			LOGGER.warn("No candidates found to replace {}", room.name().getString());
		
		room.applyTerm(term, graph);
	}
}
