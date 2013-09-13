package org.ziggrid.utils.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ziggrid.utils.exceptions.UtilException;
import org.ziggrid.utils.utils.StringUtil;


public class Reflection {
	@SuppressWarnings("unchecked")
	public static <T> T getStaticField(Class<?> inClz, String fieldName) {
		try
		{
			Field f = inClz.getDeclaredField(fieldName);
			if (f == null)
				throw new UtilException("The field '" + fieldName +"' was not defined in " + inClz);
			f.setAccessible(true);
	
			return (T) f.get(null);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static Field getFieldVar(Class<?> cls, String fieldName) {
		if (cls == null)
			throw new UtilException("Cannot use reflection on null class");
		if (fieldName == null)
			throw new UtilException("Must specify a valid field name");
		Field f = findField(cls, fieldName);
		if (f == null)
			throw new UtilException("The field '" + fieldName +"' was not defined in " + cls);
		f.setAccessible(true);
		return f;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Object target, String fieldName) {
		try
		{
			if (target == null)
				throw new UtilException("Cannot use reflection on null object");
			Class<?> clz = target.getClass();
			Field f = getFieldVar(clz, fieldName);
			return (T) f.get(target);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	@SuppressWarnings("unchecked")
	public static void setField(Object target, String fieldName, Object value) {
		try
		{
			if (target == null)
				throw new UtilException("Cannot use reflection on null object");
			if (fieldName == null)
				throw new UtilException("Must specify a valid field name");
			Class<?> clz = target.getClass();
			Field f = findField(clz, fieldName);
			if (f == null)
				throw new UtilException("The field '" + fieldName +"' was not defined in " + target.getClass());
			f.setAccessible(true);
			if (value instanceof Boolean)
				f.setBoolean(target, (Boolean)value);
			else if (f.getType().getName().equals("boolean"))
				f.setBoolean(target, Boolean.parseBoolean((String)value));
			else if (f.getType().getName().equals("int"))
				f.setInt(target, Integer.parseInt((String)value));
			else if (value == null || f.getType().isAssignableFrom(value.getClass()))
				f.set(target, value);
			else if (value != null && Collection.class.isAssignableFrom(f.getType()))
				((Collection<Object>)f.get(target)).add(value);
			else
				throw new UtilException("The field " + fieldName + " is not assignable from " + value.getClass());
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	private static Field findField(Class<?> clz, String fieldName) {
		try
		{
			return clz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException fex)
		{
			if (clz.getSuperclass() != null)
				return findField(clz.getSuperclass(), fieldName);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T create(ClassLoader loader, String cls, Object... args) {
		try
		{
			final Class<T> clz;
			if (loader != null)
				clz = (Class<T>) Class.forName(cls, false, loader);
			else
				clz = (Class<T>) Class.forName(cls);
			return create(clz, args);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static <T> T create(Class<T> clz, Object... args) {
		try
		{
			Jimmy<T>[] constructors = wrap(clz.getConstructors());
			return match(clz, "constructor", constructors, args).invoke(args);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static <O, T> T call(O invokee, String meth, Object... args) {
		try
		{
			@SuppressWarnings("unchecked")
			Class<O> clz = (Class<O>) invokee.getClass();
			Jimmy<T>[] methods = wrap(invokee, clz, meth);
			return match(clz, meth, methods, args).invoke(args);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static <O> void callSetter(O invokee, String property, Object value) {
		String meth = "set" + StringUtil.capitalize(property);
		call(invokee, meth, value);
	}

	public static Map<String, Object> callStatic(String clz, String meth, String applName) {
		try {
			return callStatic(Class.forName(clz), meth, applName);
		} catch (ClassNotFoundException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static <O, T> T callStatic(Class<O> clz, String meth, Object... args) {
		try
		{
			Jimmy<T>[] methods = wrap(clz, meth);
			return match(clz, "static " + meth, methods, args).invoke(args);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	private static <O, T> Jimmy<T> match(Class<O> clz, String what, Jimmy<T>[] jimmies, Object[] args) {
		loop:
		for (Jimmy<T> j : jimmies)
		{
			Class<?>[] jtypes = j.getTypes();
			if (args.length != jtypes.length)
				continue;
			for (int i=0;i<args.length;i++)
				if (args[i] != null && !jtypes[i].isInstance(args[i])) {
					if (jtypes[i].getSimpleName().equals("boolean") && args[i] instanceof Boolean)
						continue;
					else if (jtypes[i].getSimpleName().equals("int") && args[i] instanceof Integer)
						continue;
					else
						continue loop;
				}
			return j;
		}
		throw new UtilException("There is no matching " + what + " for class " + clz.getName() + " with args " + args.length);
	}

	// All this baloney is here because Java didn't see fit to make
	// Constructor and Method implement the same interface.  They could
	// of course, especially if they'd curried method to begin with ... sigh
	public static interface Jimmy<T> {
		public abstract T invoke(Object[] args) throws Exception;

		public abstract Class<?>[] getTypes();
	}

	public static class CJimmy<T> implements Jimmy<T> {
		private final Constructor<T> constructor;

		public CJimmy(Constructor<T> constructor) {
			this.constructor = constructor;
		}

		@Override
		public Class<?>[] getTypes() {
			return constructor.getParameterTypes();
		}

		@Override
		public T invoke(Object[] args) throws Exception {
			constructor.setAccessible(true);
			return constructor.newInstance(args);
		}
	}

	public static class MJimmy<O, T> implements Jimmy<T> {
		private final Method method;
		private final O invokee;

		public MJimmy(O invokee, Method method) {
			this.invokee = invokee;
			this.method = method;
		}

		@Override
		public Class<?>[] getTypes() {
			return method.getParameterTypes();
		}

		@SuppressWarnings("unchecked")
		@Override
		public T invoke(Object[] args) throws Exception {
			method.setAccessible(true);
			return (T) method.invoke(invokee, args);
		}
	}

	public static class SJimmy<O, T> implements Jimmy<T> {
		private final Class<O> clz;
		private final Method m;

		public SJimmy(Class<O> clz, Method m) {
			this.clz = clz;
			this.m = m;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T invoke(Object[] args) throws Exception {
			m.setAccessible(true);
			return (T) m.invoke(clz, args);
		}

		@Override
		public Class<?>[] getTypes() {
			return m.getParameterTypes();
		}

	}

	
	@SuppressWarnings("unchecked")
	private static <T> Jimmy<T>[] wrap(Constructor<?>[] constructors) {
		Jimmy<T>[] ret = new CJimmy[constructors.length];
		for (int i = 0;i<constructors.length;i++)
			ret[i] = new CJimmy<T>((Constructor<T>)constructors[i]);
		return ret;
	}

	private static <O, T> Jimmy<T>[] wrap(O invokee, Class<O> clz, String meth) {
		List<Jimmy<T>> acc = new ArrayList<Jimmy<T>>();
		Class<?> curr = clz;
		while (curr != null)
		{
			Method[] methods = curr.getDeclaredMethods();
			for (Method m : methods)
			{
				if (m.getName().equals(meth))
					acc.add(new MJimmy<O, T>(invokee, m));
			}
			curr = curr.getSuperclass();
		}
		@SuppressWarnings("unchecked")
		Jimmy<T>[] ret = acc.toArray(new Jimmy[acc.size()]);
		return ret;
	}

	private static <O, T> Jimmy<T>[] wrap(Class<O> clz, String meth) {
		List<Jimmy<T>> acc = new ArrayList<Jimmy<T>>();
		Class<?> curr = clz;
		while (curr != null)
		{
			Method[] methods = curr.getDeclaredMethods();
			for (Method m : methods)
			{
				if (m.getName().equals(meth))
					acc.add(new SJimmy<O, T>(clz, m));
			}
			curr = curr.getSuperclass();
		}
		@SuppressWarnings("unchecked")
		Jimmy<T>[] ret = acc.toArray(new Jimmy[acc.size()]);
		return ret;
	}

	public static Map<Field, Object> allFields(Object inpf) {
		HashMap<Field, Object> ret = new HashMap<Field, Object>();
		if (inpf == null)
			return ret;
		Class<?> clz = inpf.getClass();
		while (clz != Object.class) {
			for (Field f : clz.getDeclaredFields())
			{
				try {
					f.setAccessible(true);
					ret.put(f, f.get(inpf));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			clz = clz.getSuperclass();
		}
		return ret;
	}
}
