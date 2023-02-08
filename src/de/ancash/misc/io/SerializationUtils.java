package de.ancash.misc.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Assists with the serialization process and performs additional functionality
 * based on serialization.
 * </p>
 *
 * <ul>
 * <li>Deep clone using serialization
 * <li>Serialize managing finally and IOException
 * <li>Deserialize managing finally and IOException
 * </ul>
 *
 * <p>
 * This class throws exceptions for invalid {@code null} inputs. Each method
 * documents its behavior in more detail.
 * </p>
 *
 * <p>
 * #ThreadSafe#
 * </p>
 * 
 * @since 1.0
 */
public class SerializationUtils {
	
	static int i = 12;
	
	private static final Set<ClassLoader> clazzLoader = new HashSet<>();

	static {
		clazzLoader.add(ClassLoader.getSystemClassLoader());
	}
	
	public static void addClazzLoader(ClassLoader cl) {
		clazzLoader.add(cl);
	}
	
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

	/**
	 * <p>
	 * Custom specialization of the standard JDK {@link java.io.ObjectInputStream}
	 * that uses a custom {@code ClassLoader} to resolve a class. If the specified
	 * {@code ClassLoader} is not able to resolve the class, the context classloader
	 * of the current thread will be used. This way, the standard deserialization
	 * work also in web-application containers and application servers, no matter in
	 * which of the {@code ClassLoader} the particular class that encapsulates
	 * serialization/deserialization lives.
	 * </p>
	 *
	 * <p>
	 * For more in-depth information about the problem for which this class here is
	 * a workaround, see the JIRA issue LANG-626.
	 * </p>
	 */
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
		public ClassLoaderAwareObjectInputStream(final InputStream in, final ClassLoader classLoader)
				throws IOException {
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

	/**
	 * <p>
	 * SerializationUtils instances should NOT be constructed in standard
	 * programming. Instead, the class should be used as
	 * {@code SerializationUtils.clone(object)}.
	 * </p>
	 *
	 * <p>
	 * This constructor is public to permit tools that require a JavaBean instance
	 * to operate.
	 * </p>
	 * 
	 * @since 2.0
	 */
	public SerializationUtils() {
	}

}
