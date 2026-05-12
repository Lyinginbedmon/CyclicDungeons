package com.lying.grid;

import java.util.List;
import java.util.Optional;

/** A result handler class, so we can readily distinguish between an empty path and a failed path */
public class PathingResult
{
	protected final List<GridTile> contents;
	protected final Optional<String> reasonForFailure;
	
	protected PathingResult(List<GridTile> tiles, String reasonIn)
	{
		contents = tiles;
		reasonForFailure = tiles.isEmpty() ? Optional.of(reasonIn) : Optional.empty();
	}
	
	public static PathingResult failure(String reason)
	{
		return new PathingResult(List.of(), reason);
	}
	
	public static PathingResult success(List<GridTile> tiles)
	{
		return new PathingResult(tiles, "");
	}
	
	public boolean isSuccess() { return !isFailure(); }
	
	public boolean isFailure() { return contents.isEmpty(); }
	
	public List<GridTile> result() { return contents; }
	
	/** Returns the number of tiles this path uses */
	public int size() { return contents.size(); }
	
	public String failureReason() { return reasonForFailure.orElse("NULL"); }
}