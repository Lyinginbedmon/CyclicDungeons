package com.lying.utility;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.lying.reference.Reference;

/** Used during debug operations to only print results when an error occurs at the end of a calculation */
public class QueuedLogger extends DebugLogger
{
	public static final QueuedLogger INSTANCE = new QueuedLogger("debug_queue");
	
	private List<Consumer<QueuedLogger>> queue = Lists.newArrayList();
	private final Logger logger;
	
	public QueuedLogger(String sub)
	{
		super(sub);
		logger = LoggerFactory.getLogger(Reference.ModInfo.MOD_ID+"_"+sub.toLowerCase());
	}
	
	public void clear() { queue.clear(); }
	
	public void report()
	{
		queue.forEach(c -> c.accept(this));
		clear();
	}
	
	public void info(String msg) { queue.add(l -> l.logger.info(msg)); }
	public void info(String format, Object... args) { queue.add(l -> l.logger.info(format, args)); }
	
	public void error(String msg) { queue.add(l -> l.logger.error(" !! "+msg)); }
	public void error(String format, Object... args) { queue.add(l -> l.logger.error(" !! "+format, args)); }
	
	public void warn(String msg) { queue.add(l -> l.logger.warn(" ? "+msg)); }
	public void warn(String format, Object... args) { queue.add(l -> l.logger.warn(" ? "+format, args)); }
	
	public void forceWarn(String msg) { queue.add(l -> l.logger.warn(" ?? "+msg)); }
	public void forceWarn(String format, Object... args) { queue.add(l -> l.logger.warn(" ?? "+format, args)); }
}
