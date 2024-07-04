package de.ancash.misc.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SerializationUtils {

//	public static void main(String[] args) throws IOException, ClassNotFoundException {
//
//		Map<Object, Object> test = new HashMap<Object, Object>();
//
//		for (int i = 0; i < 100; i++) {
//			test.put(i, i);
//			test.put(i + 1000, new byte[10]);
//
//		}
//
//		byte[] ser = null;
//
//		int i = 100_000;
//		long now = System.nanoTime();
//		for (int a = 0; a < i; a++) {
//			ser = serializeToBytes(test);
//		}
//
//		System.out.println((System.nanoTime() - now) / i + " ns/write old");
//
//		now = System.nanoTime();
//		for (int a = 0; a < i; a++) {
//			deserializeFromBytes(ser);
//		}
//
//		System.out.println((System.nanoTime() - now) / i + " ns/read old");
//
//		now = System.nanoTime();
//		for (int a = 0; a < i; a++) {
//			ser = conf.asByteArray(test);
//		}
//
//		System.out.println((System.nanoTime() - now) / i + " ns/write new");
//
//		now = System.nanoTime();
//		for (int a = 0; a < i; a++) {
//			conf.asObject(ser);
//		}
//
//		System.out.println((System.nanoTime() - now) / i + " ns/read new");
//
//	}

//	static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
	private static final Set<ClassLoader> clazzLoader = new HashSet<>();

//	static {
//		clazzLoader.add(ClassLoader.getSystemClassLoader());
//		try {
//			Field f = org.nustaq.serialization.FSTConfiguration.class.getDeclaredField("classRegistry");
//			f.setAccessible(true);
//			f.set(conf, new FSTClazzNameRegistry(null));
//			Method m = FSTConfiguration.class.getDeclaredMethod("addDefaultClazzes");
//			m.setAccessible(true);
//			m.invoke(conf);
//		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
//				| NoSuchMethodException | InvocationTargetException e1) {
//			e1.printStackTrace();
//		}
//		conf.setLastResortResolver(new LastResortClassResolver() {
//			@Override
//			public Class<?> getClass(String clName) {
//				Class<?> clazz = getClazz(clName);
//				if(clazz == null)
//					return Unknown.class;
//				return clazz;
//			}
//		});
//	}

	private static final ConcurrentHashMap<String, Class<?>> clazzRegistry = new ConcurrentHashMap<String, Class<?>>();

	public static Class<?> getClazz(String clName) {
		if (clazzRegistry.containsKey(clName))
			return clazzRegistry.get(clName);
		for (ClassLoader cl : clazzLoader) {
			Class<?> clazz;
			try {
				clazz = Class.forName(clName, true, cl);
			} catch (ClassNotFoundException e) {
				continue;
			}
			if (clazz != null) {
				clazzRegistry.put(clName, clazz);
				return clazz;
			}
		}
		return null;
	}

	public static void addClazzLoader(ClassLoader cl) {
		clazzLoader.add(cl);
	}

//	public static Object deserializeFST(byte[] bytes) {
//		return conf.asObject(bytes);
//	}
//
//	public static byte[] serializeFST(Object o) {
//		return conf.asByteArray(o);
//	}

	public static Serializable deserializeWithClassLoaders(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		Exception e = null;
		for (ClassLoader cl : clazzLoader) {
			try (ClassLoaderAwareObjectInputStream classLoaderAwareOIS = new ClassLoaderAwareObjectInputStream(b, cl)) {
				Serializable s = (Serializable) classLoaderAwareOIS.readObject();
				return s;
			} catch (Exception ex) {
				e = ex;
				b.reset();
			}
		}
		throw new ClassNotFoundException(e.getLocalizedMessage(), e);
	}

	@SuppressWarnings("nls")
	public static class ClassLoaderAwareObjectInputStream extends ObjectInputStream {
		private static final Map<String, Class<?>> primitiveTypes = new HashMap<>();

		static {
			primitiveTypes.put("byte", byte.class);
			primitiveTypes.put("short", short.class);
			primitiveTypes.put("int", int.class);
			primitiveTypes.put("long", long.class);
			primitiveTypes.put("float", float.class);
			primitiveTypes.put("double", double.class);
			primitiveTypes.put("boolean", boolean.class);
			primitiveTypes.put("char", char.class);
			primitiveTypes.put("void", void.class);
		}

		private final ClassLoader classLoader;

		/**
		 * Constructor.
		 * 
		 * @param in          The {@code InputStream}.
		 * @param classLoader classloader to use
		 * @throws IOException if an I/O error occurs while reading stream header.
		 * @see java.io.ObjectInputStream
		 */
		public ClassLoaderAwareObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
			super(in);
			this.classLoader = classLoader;
		}

		/**
		 * Overridden version that uses the parameterized {@code ClassLoader} or the
		 * {@code ClassLoader} of the current {@code Thread} to resolve the class.
		 * 
		 * @param desc An instance of class {@code ObjectStreamClass}.
		 * @return A {@code Class} object corresponding to {@code desc}.
		 * @throws IOException            Any of the usual Input/Output exceptions.
		 * @throws ClassNotFoundException If class of a serialized object cannot be
		 *                                found.
		 */
		@Override
		protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			final String name = desc.getName();
			try {
				Class<?> clazz = getClazz(name);
				if (clazz != null)
					return clazz;
				return Class.forName(name, true, classLoader);
			} catch (final ClassNotFoundException ex) {
				try {
					return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
				} catch (final ClassNotFoundException cnfe) {
					final Class<?> cls = primitiveTypes.get(name);
					if (cls != null) {
						return cls;
					}
					throw new ClassNotFoundException(name, cnfe);
				}
			}
		}

	}

	public static byte[] serializeToBytes(Object obj) throws IOException {
		try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
			try (ObjectOutputStream o = new ObjectOutputStream(b)) {
				o.writeObject(obj);
			}
			return b.toByteArray();
		}
	}

	public static Object deserializeFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
			try (ObjectInputStream o = new ObjectInputStream(b)) {
				return o.readObject();
			}
		}
	}
}
