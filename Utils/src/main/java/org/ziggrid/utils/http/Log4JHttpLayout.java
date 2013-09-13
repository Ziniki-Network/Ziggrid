package org.ziggrid.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class Log4JHttpLayout extends Layout {
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
	private static final int maxNameLength = 18;

	public Log4JHttpLayout()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@Override
	public String format(LoggingEvent event) {
		long time = event.getTimeStamp();
		String name = event.getLoggerName();
		if (name.length() < maxNameLength)
			name = "                 " + name;
		name = name.substring(name.length() - maxNameLength);
		Level level = event.getLevel();

		//TODO: consider using event.getThrowableStringRep instead. It returns a string array, so we'll need to figure out how that compares to a stack trace first.
		String exInfo = "";
		ThrowableInformation throwableInformation = event.getThrowableInformation();
		if (throwableInformation != null)
		{
			//Being a little extra cautious here. In some cases this throwable is null even when the throwableinformation is not.
			Throwable ex = throwableInformation.getThrowable();
			if(ex != null)
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream sw = new PrintStream(baos);
				ex.printStackTrace(sw);
				exInfo = baos.toString();
			}
		}
		
		String thread = event.getThreadName();
		return sdf.format(new Date(time)) + " " + name + "/" + thread.substring(thread.length()-2) + " " + level + ": " + event.getMessage() + "\n" + exInfo;
	}

	@Override
	public void activateOptions() {
		// TODO: I don't think we have any of these
		
	}

	@Override
	public boolean ignoresThrowable() {
		return false;
	}

}
