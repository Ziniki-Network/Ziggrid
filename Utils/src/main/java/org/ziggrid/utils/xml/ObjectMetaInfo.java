package org.ziggrid.utils.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.ziggrid.utils.exceptions.InvalidXMLTagException;
import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.exceptions.XMLProcessingException;

// I would expect this to be capable of handling sensible defaults
class ObjectMetaInfo {
	private final Object callbacks;
	public final boolean wantsText;
	private CallbackTable callbackTable;

	public ObjectMetaInfo(Object callbacks) {
		this.callbacks = callbacks;
		Class<?> clz = callbacks.getClass();
		XMLWants annotation = clz.getAnnotation(XMLWants.class);
		if (annotation != null)
		{
			if (annotation.value() == XMLWant.ELEMENTS)
				wantsText = false;
			else
			{
				if (!(callbacks instanceof XMLTextReceiver))
					throw new UtilException("The class " + clz + " cannot receive text because it does not implement XMLTextReceiver");
				wantsText = true;
			}
		}
		else
			wantsText = (callbacks instanceof XMLTextReceiver) || (callbacks instanceof XMLContextTextReceiver);
		callbackTable = new CallbackTable(clz);
				
	}

	public Object dispatch(Object cxt, XMLElement xe) {
		// There should be ordering checks
		// There should be an indirection table
		// There should be checks that the method exists
		
		if (callbacks instanceof XMLElementReceiver)
		{
			return ((XMLElementReceiver)callbacks).receiveElement(xe);
		}
		String tag = xe.tag().toLowerCase();
		return callbackTable.invoke(callbacks, tag, cxt, xe);
	}
}

class CallbackTable {
	static class MethodMetaInfo {
		public final Method method1;
		public final Method method2;
		
		public MethodMetaInfo(Method m1, Method m2)
		{
			method1 = m1;
			if (m1 != null)
				method1.setAccessible(true);
			method2 = m2;
			if (m2 != null)
				method2.setAccessible(true);
		}
	}
	
	private Map<String, MethodMetaInfo> callbackTable = new HashMap<String, MethodMetaInfo>();

	public CallbackTable(Class<?> clz) {
		while (clz != Object.class)
		{
			for (Method m : clz.getDeclaredMethods())
			{
				// System.out.println(m);
				Class<?>[] ptypes = m.getParameterTypes();
				Method method1 = null;
				Method method2 = null;
				String name = m.getName().toLowerCase();
				if (callbackTable.containsKey(name))
				{
					MethodMetaInfo minfo = callbackTable.get(name);
					method1 = minfo.method1;
					method2 = minfo.method2;
				}
				if (Object.class.isAssignableFrom(m.getReturnType()) && ptypes.length == 1 && ptypes[0].equals(XMLElement.class))
					method1 = m;
				if (Object.class.isAssignableFrom(m.getReturnType()) && ptypes.length == 2 && ptypes[1].equals(XMLElement.class))
					method2 = m;
				
				if (method1 != null || method2 != null)
					callbackTable.put(name, new MethodMetaInfo(method1, method2));
			}
			clz = clz.getSuperclass();
		}
	}

	public Object invoke(Object callbacks, String which, Object cxt, XMLElement xe)
	{
		if (callbacks == null)
			throw new UtilException("Cannot invoke " + which + " on a null object");
		if (!callbackTable.containsKey(which)) {
			InvalidXMLTagException ex = new InvalidXMLTagException(xe, which, callbacks);
			if (xe.hasHandler())
				xe.getHandler().invalidTag(xe.getStartLocation(), xe.getEndLocation(), ex);
			throw ex;
		}
		try
		{
			MethodMetaInfo minfo = callbackTable.get(which);
			if (minfo.method2 != null)
				return minfo.method2.invoke(callbacks, cxt, xe);
			if (minfo.method1 == null)
				throw new UtilException("There is no invocation method for " + which + " on " + callbacks);
			return minfo.method1.invoke(callbacks, xe);
		}
		catch (InvocationTargetException ex) {
			Throwable tmp = ex.getCause();
			if (tmp instanceof XMLProcessingException && xe.hasHandler())
				throw (RuntimeException)tmp;
			throw UtilException.wrap(ex);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
}
