package com.lying.utility.logging;

import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.Lists;

public class DataLog 
{
	private List<LogEntry> entries = Lists.newArrayList();
	
	public void clear() { entries.clear(); }
	
	public void info(String format, Object... args)
	{
		entries.add(new LogEntry(LogEntry.Type.INFO, format, args));
	}
	
	public void warn(String format, Object... args)
	{
		entries.add(new LogEntry(LogEntry.Type.WARN, format, args));
	}
	
	public void error(String format, Object... args)
	{
		entries.add(new LogEntry(LogEntry.Type.ERROR, format, args));
	}
	
	public void report(Logger logger)
	{
		entries.forEach(log -> 
		{
			switch(log.type)
			{
				case ERROR:
					logger.error(log.message());
					break;
				case INFO:
					logger.info(log.message());
					break;
				case WARN:
					logger.warn(log.message());
					break;
			}
		});
	}
	
	private class LogEntry
	{
		public static final String ARG = "{}";
		private final String text;
		private final Object[] args;
		private final Type type;
		
		public LogEntry(Type typeIn, String textIn, Object... argsIn)
		{
			text = textIn;
			type = typeIn;
			args = argsIn;
		}
		
		public String message()
		{
			if(args.length == 0 || text.indexOf(ARG) < 0)
				return text;
			
			String message = text;
			for(int i=0; i<args.length; i++)
			{
				int index = message.indexOf(ARG);
				if(index < 0)
					break;
				
				String val = args[i].toString();
				message = 
						message.substring(0, index) + 
						val + 
						message.substring(index + ARG.length());
			}
			
			return message;
		}
		
		public static enum Type
		{
			INFO,
			WARN,
			ERROR;
		}
	}
}
