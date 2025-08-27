package com.lying.grammar;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;

import com.lying.CyclicDungeons;

public class Grammar
{
	private static final Logger LOGGER = CyclicDungeons.LOGGER;
	
	public static final List<Term> PLACEABLE = CDTerms.placeables();
	
	public static void run()
	{
		final int scale = 5;
		LOGGER.info("Initial graph: {}", initialGraph(scale).describe().getString());
		for(int i=0; i<5; i++)
			LOGGER.info("Generated graph {}: {}", i, generate(initialGraph(scale)).describe().getString());
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
		{
			/** Previous room evaluated */
			Room prev = graph.get(0).get();
			
			while(prev.hasLink() && prev.depth <= graph.depth())
			{
				/** If the room has no links, we know it's a dead end */
				Optional<Room> linkOpt = graph.get(prev.getLink().get());
				if(linkOpt.isEmpty())
				{
					LOGGER.warn("Unrecognised room ID in graph");
					break;
				}
				
				/** Room being evaluated */
				Room current = linkOpt.get();
				
				if(current.isReplaceable())
				{
					final Room previous = prev;
					
					/** Room immediately following this one */
					final Room next = graph.get(current.getLink().get()).orElse(null);
					
					Term term = CDTerms.VOID.get();
					List<Term> candidates = PLACEABLE.stream().filter(t -> t.canBePlaced(current, previous, next, graph)).toList();
					if(!candidates.isEmpty())
						term = candidates.size() == 1 ? candidates.get(0) : candidates.get(rand.nextInt(candidates.size()));
					else
						LOGGER.warn("No candidates found to replace {}", current.name().getString());
					
					current.applyTerm(term, graph);
				}
				prev = current;
			}
		}
		return graph;
	}
}
