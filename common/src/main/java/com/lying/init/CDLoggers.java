package com.lying.init;

import com.lying.utility.DebugLogger;

/** Centralised debug logger control class */
public class CDLoggers
{
	public static final DebugLogger GRAMMAR		= DebugLogger.of("grammar").disable();
	public static final DebugLogger PLANAR		= DebugLogger.of("planar").disable();
	public static final DebugLogger WFC			= DebugLogger.of("wfc").disable();
	public static final DebugLogger WORLDGEN	= DebugLogger.of("worldgen").enable();
}
