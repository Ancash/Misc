package de.ancash.misc.yaml;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.simpleyaml.configuration.serialization.ConfigurationSerializable;
import org.simpleyaml.configuration.serialization.ConfigurationSerialization;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class YamlAnnotationProcessor {

	private static final Map<Class<?>, Function<Object, Object>> commonSerializer = new HashMap<Class<?>, Function<Object, Object>>();
	private static final Map<Class<?>, BiFunction<Class<?>, Object, Object>> commonDeserializer = new HashMap<Class<?>, BiFunction<Class<?>, Object, Object>>();

	static {
		commonSerializer.put(Enum.class, o -> ((Enum) o).name());
		commonDeserializer.put(Enum.class, (c, e) -> e instanceof Enum ? (Enum) e : Enum.valueOf((Class<? extends Enum>) c, (String) e));
		commonSerializer.put(UUID.class, Object::toString);
		commonDeserializer.put(UUID.class, (c, u) -> UUID.fromString((String) u));
	}

//	public static void main(String[] args) throws IOException {
//		Test t = new Test();
//		t.test.add(A.B);
//		t.test.add(A.C);
//		YamlFile y = new YamlFile();
//		y.set("a", t.serialize());
//		System.out.println(y.saveToString());
//		Test d = Test.deserialize(t.serialize());
//		System.out.println(d.serialize());
//		y.set("b", d.serialize());
//		System.out.println(y.saveToString());
//		System.out.println(d.test.stream().findAny().get().getClass().getCanonicalName());
//		System.out.println(d.lol);
//	}
//
//	static class Test implements ConfigurationSerializable {
//
//		@YamlSerializable(key = "test")
//		Set<A> test = new HashSet<A>();
//		@YamlSerializable(key = "arr")
//		int[] lol = new int[2];
//
//		@Override
//		public Map<String, Object> serialize() {
//			return YamlAnnotationProcessor.serialize(this);
//		}
//
//		public static Test deserialize(Map<String, Object> m) {
//			return loadInto(m, new Test());
//		}
//	}
//
//	
//	public enum A{
//		B, C;
//	}

	public static Map<String, Object> serialize(ConfigurationSerializable cs) {
		Map<String, Object> serialized = new HashMap<String, Object>();
		Class<?> clazz = cs.getClass();
		while (clazz != null && !clazz.equals(Object.class)) {
			for (Field field : clazz.getDeclaredFields()) {
				if (!field.isAnnotationPresent(YamlSerializable.class))
					continue;
				YamlSerializable annotation = field.getAnnotation(YamlSerializable.class);
				if (serialized.containsKey(annotation.key()))
					throw new IllegalStateException("duplicate key " + annotation.key());
				try {
					field.setAccessible(true);
					Object val = field.get(cs);
					if (val == null)
						continue;
					serialized.put(annotation.key(), serializeObject(val));
				} catch (Throwable e) {
					throw new IllegalStateException("could not serialize " + cs.getClass() + ":" + field.getName() + " (" + annotation.key() + ")",
							e);
				}
			}
			clazz = clazz.getSuperclass();
		}
		return serialized;
	}

	private static Object serializeObject(Object val) {
		if (val instanceof ConfigurationSerializable) {
			Map<String, Object> v = ((ConfigurationSerializable) val).serialize();
			v.put("==", val.getClass().getCanonicalName());
			return v;
		} else if (val.getClass().isEnum())
			return commonSerializer.get(Enum.class).apply(val);
		else if (commonSerializer.containsKey(val.getClass()))
			return commonSerializer.get(val.getClass()).apply(val);
		else if (List.class.isAssignableFrom(val.getClass()) || Set.class.isAssignableFrom(val.getClass()))
			return ((Collection<?>) val).stream().map(YamlAnnotationProcessor::serializeObject).collect(Collectors.toList());
		else if (val.getClass().isArray()) {
			List<Object> elements = new ArrayList<Object>();
			for (int i = 0; i < Array.getLength(val); i++) {
				Object o = Array.get(val, i);
				elements.add(o == null ? null : serializeObject(o));
			}
			val = elements;
		}

		return val;
	}

	public static <T extends ConfigurationSerializable> T loadInto(Map<String, Object> map, T override) {
		try {
			Class<?> clazz = override.getClass();
			while (!map.isEmpty() && !clazz.equals(Object.class)) {
				Map<String, Field> fields = new HashMap<String, Field>();
				Arrays.asList(clazz.getDeclaredFields()).stream().filter(f -> f.isAnnotationPresent(YamlSerializable.class))
						.forEach(f -> fields.put(f.getAnnotation(YamlSerializable.class).key(), f));
				Iterator<String> iter = map.keySet().iterator();
				while (iter.hasNext()) {
					String key = iter.next();
					if (fields.containsKey(key)) {
						Object val = map.get(key);
						iter.remove();
						Field field = fields.get(key);
						field.setAccessible(true);
						deserializeField(field, override, val);
					}
				}
				clazz = clazz.getSuperclass();
			}
			return override;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	private static void deserializeField(Field field, Object override, Object val) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<?> ft = field.getType();
		Type type = field.getGenericType();
		Class<?>[] generics = new Class<?>[0];

		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type[] types = pt.getActualTypeArguments();
			generics = new Class<?>[types.length];
			for (int i = 0; i < types.length; i++) {
				Type t = types[i];
				if (t instanceof WildcardType)
					throw new UnsupportedOperationException(
							override.getClass() + ":" + field.getName() + ":" + field.getType().getCanonicalName() + " wildcard not supported");
				generics[i] = (Class<?>) t;
			}
		} else if (ft.isArray()) {
			generics = new Class<?>[] { field.getType().getComponentType() };
		}
		field.set(override, deserializeObject(field.getType(), generics, val));
	}

	private static Object deserializeObject(Class<?> fieldType, Class<?>[] generics, final Object val) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (val instanceof Map) {
			Map<?, Object> m = (Map<Object, Object>) val;
			if (m.containsKey("=="))
				return ConfigurationSerialization.deserializeObject((Map<String, Object>) m);
		} else if (fieldType.isEnum()) {
			return commonDeserializer.get(Enum.class).apply(fieldType, val);
		} else if (commonDeserializer.containsKey(fieldType)) {
			return commonDeserializer.get(fieldType).apply(fieldType, val);
		} else if (List.class.isAssignableFrom(fieldType) || Set.class.isAssignableFrom(fieldType)) {
			Class<?> collType = null;
			if (fieldType.equals(List.class))
				collType = ArrayList.class;
			else if (fieldType.equals(Set.class))
				collType = HashSet.class;
			else
				collType = fieldType;
			Collection nt = (Collection<?>) collType.getConstructor().newInstance();
			Collection collVal = (Collection<?>) val;
			for (Object o : collVal)
				nt.add(deserializeObject(generics[0], generics.length == 1 ? null : Arrays.copyOfRange(generics, 1, generics.length), o));
			return nt;
		} else if (fieldType.isArray()) {
			Function<Integer, Object> mapper;
			if (val.getClass().isArray())
				mapper = index -> Array.get(val, index);
			else
				mapper = index -> ((List<?>) val).get(index);
			Object arr = Array.newInstance(generics[0], val.getClass().isArray() ? Array.getLength(val) : ((List<?>) val).size());
			for (int i = 0; i < Array.getLength(arr); i++) {
				Object o = mapper.apply(i);
				Array.set(arr, i, deserializeObject(generics[0], generics.length == 1 ? null : Arrays.copyOfRange(generics, 1, generics.length), o));
			}
			return arr;
		}

		return val;
	}
}
