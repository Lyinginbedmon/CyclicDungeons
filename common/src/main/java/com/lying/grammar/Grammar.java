package com.lying.grammar;

import java.util.List;
import java.util.Random;

import org.slf4j.Logger;

import com.lying.CyclicDungeons;
import com.lying.init.CDTerms;

public class Grammar
{
	private static final Logger LOGGER = CyclicDungeons.LOGGER;
	
	public static final List<Term> PLACEABLE = CDTerms.placeables();
	
	public static void run()
	{
		final int scale = 5;
		LOGGER.info("Initial graph: {}", initialGraph(scale).asString());
		for(int i=0; i<5; i++)
			LOGGER.info("Generated graph {}: {}", i, generate(initialGraph(scale)).asString());
	}
	
	/** Generates a linear initial starting graph */
	public static Graph initialGraph(int blanks)
	{
		Graph graph = new Graph();
		Room start = new Room().setTerm(CDTerms.START.get());
		graph.add(start);
		
		Room previous = start;
		for(int i=0; i<blanks; i++)
		{
			Room blank = new Room();
			previous.linkTo(blank.uuid());
			
			previous = blank;
			graph.add(blank);
		}
		
		Room end = new Room().setTerm(CDTerms.END.get());
		previous.linkTo(end.uuid());
		graph.add(end);
		return graph;
	}
	
	/** Populates the given graph */
	public static Graph generate(Graph graph)
	{
		Random rand = new Random();
		int iterationCap = 5;
		while(!graph.isEmpty() && graph.hasBlanks() && --iterationCap > 0)
			recursiveGenerate(graph.get(0).get(), graph, rand);
		return graph;
	}
	
	private static void recursiveGenerate(Room room, Graph graph, Random rand)
	{
		generate(room, graph, rand);
		room.getLinksFrom(graph).forEach(r -> recursiveGenerate(r, graph, rand));
	}
	
	private static void generate(Room room, Graph graph, Random rand)
	{
		if(!room.isReplaceable())
			return;
		
		/** Rooms that connect to this one */
		final List<Room> previous = graph.getLinksTo(room.uuid());
		
		/** Rooms immediately following this one */
		final List<Room> next = room.getLinksFrom(graph);
		
		Term term = CDTerms.VOID.get();
		List<Term> candidates = PLACEABLE.stream().filter(t -> t.canBePlaced(room, previous, next, graph)).toList();
		if(!candidates.isEmpty())
			term = candidates.size() == 1 ? candidates.get(0) : candidates.get(rand.nextInt(candidates.size()));
		else
			LOGGER.warn("No candidates found to replace {}", room.name().getString());
		
		room.applyTerm(term, graph);
	}
}
