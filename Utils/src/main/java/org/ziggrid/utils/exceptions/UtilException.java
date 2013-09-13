package org.ziggrid.utils.exceptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("serial")
public class UtilException extends RuntimeException {

	public UtilException(String string) {
		super(string);
	}

	public UtilException(String string, Throwable ex) {
		super(string, ex);
	}

	public static RuntimeException wrap(Throwable ex) {
		if (ex instanceof RuntimeException)
			return (RuntimeException)ex;
		else if (ex instanceof InvocationTargetException || ex instanceof ExecutionException)
			return wrap(ex.getCause());
		else
			return new UtilException("A checked exception was caught", ex);
	}
	
	public static Throwable unwrap(Exception ex) {
		if (ex instanceof UtilException && ex.getCause() != null)
			return ex.getCause();
		else if (ex instanceof InvocationTargetException || ex instanceof ExecutionException)
			return ex.getCause();
		else
			return ex;
	}

	public static Exception reconstitute(String exClass, String msg) {
		if (exClass == null)
			return new UtilException("Exception occurred, but no details provided");
		try
		{
			Class<?> forName = Class.forName(exClass);
			try
			{
				Constructor<?> ctor = forName.getDeclaredConstructor(String.class);
				ctor.setAccessible(true);
				if (ctor != null)
					return (Exception) ctor.newInstance(msg);
			}
			catch (NoSuchMethodException e2)
			{
			}
			return (Exception) forName.newInstance();
		}
		catch (Exception ex)
		{
			return new UtilException("Exception of unrecognized class " + exClass + " thrown by server", ex);
		}
	}
}
