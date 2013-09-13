package org.ziggrid.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class HttpFormatter extends Formatter {
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
	private static final int maxNameLength = 18;

	public HttpFormatter()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@Override
	public String format(LogRecord record) {
		long time = record.getMillis();
		String name = record.getLoggerName();
		if (name.length() < maxNameLength)
			name = "                 " + name;
		name = name.substring(name.length() - maxNameLength);
		Level level = record.getLevel();
		Throwable ex = record.getThrown();
		String exInfo = "";
		if (ex != null)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream sw = new PrintStream(baos);
			ex.printStackTrace(sw);
			exInfo = baos.toString();
		}
		return sdf.format(new Date(time)) + " " + name + "[" + record.getThreadID() + "] " + level + ": " + record.getMessage() + "\n" + exInfo;
	}
}
