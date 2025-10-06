package com.lying.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lying.reference.Reference;

/** Middle-man class to optionally reduce/increase debug logging */
public class DebugLogger
{
	private final Logger logger;
	private boolean isActive = true;
	
	public DebugLogger(String sub)
	{
		logger = LoggerFactory.getLogger(Reference.ModInfo.MOD_ID+"_"+sub.toLowerCase());
	}
	
	public static DebugLogger of(String sub)
	{
		return new DebugLogger(sub);
	}
	
	public DebugLogger setActive(boolean bool)
	{
		isActive = bool;
		return this;
	}
	
	public DebugLogger disable()
	{
		return setActive(false);
	}
	
	public DebugLogger enable()
	{
		return setActive(true);
	}
	
	// Info - Conditionally transmitted
	public void info(String msg) { if(!isActive) return; logger.info(msg); }
	public void info(String format, Object... args) { if(!isActive) return; logger.info(format, args); }
	
	// Errors - Always transmitted
	public void error(String msg) { logger.error(msg); }
	public void error(String format, Object... args) { logger.error(format, args); }
	
	// Warnings - Conditionally transmitted
	public void warn(String msg) { if(!isActive) return; logger.warn(msg); }
	public void warn(String format, Object... args) { if(!isActive) return; logger.warn(format, args); }
	
	// Vital warnings - Always transmitted
	public void forceWarn(String msg) { logger.warn(msg); }
	public void forceWarn(String format, Object... args) { logger.warn(format, args); }
}
