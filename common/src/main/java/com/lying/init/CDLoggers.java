package com.lying.init;

import com.lying.utility.DebugLogger;

/** Centralised debug logger control class */
public class CDLoggers
{
	public static final DebugLogger GRAMMAR		= DebugLogger.of("grammar");
	public static final DebugLogger PLANAR		= DebugLogger.of("planar");
	public static final DebugLogger WFC			= DebugLogger.of("wfc");
	public static final DebugLogger WORLDGEN	= DebugLogger.of("worldgen");
}
