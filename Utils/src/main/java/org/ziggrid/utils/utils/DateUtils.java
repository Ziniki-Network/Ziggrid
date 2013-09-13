package org.ziggrid.utils.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.ziggrid.utils.exceptions.UtilException;

public class DateUtils {
	public enum Format {
		hhmmss3, sss3, isodate;

		public String format(long elapsed) {
			final int millis = (int)elapsed%1000;
			final int inSeconds = (int) (elapsed/1000);
			final int seconds = (int) (inSeconds%60);
			final int minutes = (int) ((elapsed/60000)%60);
			final int hours = (int) ((elapsed/3600000));
			
			switch (this) {
			case hhmmss3:
			{
				return StringUtil.concat(StringUtil.digits(hours,2), ":", StringUtil.digits(minutes,2), ":", StringUtil.digits(seconds,2), ".", StringUtil.digits(millis, 3));
			}
			case sss3:
			{
				return StringUtil.concat(Integer.toString(inSeconds), ".", StringUtil.digits(millis, 3));
			}
			default:
				throw new UtilException("The format " + this + " is not handled in format");
			}
		}
		
		public String showDate(Date d) {
			switch (this) {
			case isodate:
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
				format.setTimeZone(TimeZone.getTimeZone("UTC"));
				return format.format(d);
			default:
				throw new UtilException("The format " + this + " is not handled in showDate");
			}
		}
	}

	public static String elapsedTime(Date from, Date to, Format fmt) {
		long fromMillis = from.getTime();
		long toMillis = to.getTime();
		long elapsed = toMillis-fromMillis;
		
		return fmt.format(elapsed);
	}

	public static Date add(Date date, int ms) {
		return new Date(date.getTime() + ms);
	}
	
	public static class Timer
	{
		private Date start;

		public Timer()
		{
			start = new Date();
		}
		
		public boolean notYet(long msElapsed) {
			return elapsedMs() < msElapsed;
		}
		
		public String getElapsed(Format format)
		{
			return elapsedTime(start, new Date(), format);
		}

		public long elapsedMs() {
			return new Date().getTime()-start.getTime();
		}
	}
}
