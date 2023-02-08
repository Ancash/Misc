
package de.ancash.misc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

public final class ReflectionUtils{
   
	public static String toString(Object o) {
		return toString(o, false);
	}
	
	public static String toString(Object o, boolean lineSeperator) {
		StringBuilder builder = new StringBuilder();
		if(o == null) {
			builder.append("null");
			return builder.toString();
		}
		
		if(lineSeperator)
			builder.append(System.lineSeparator());
		builder.append(o.getClass().getName());
		builder.append("{");
		if(lineSeperator)
			builder.append(System.lineSeparator());
		
		Field[] fields = o.getClass().getDeclaredFields();
		
		for(Field f : fields) {
			if(!f.isAccessible())
				try {
					f.setAccessible(true);
				} catch(Exception e) {
					continue;
				}
			builder.append((lineSeperator ? "" : ";") + f.getName());
			builder.append("=");
			try {
				Object val = f.get(o);
				if(val != null && val.getClass().isArray()) {
					Object[] arr = new Object[Array.getLength(val)];
					for(int i = 0; i<arr.length; i++)
						arr[i] = Array.get(val, i);
					val = Arrays.asList(arr);
				}
				builder.append(val);
			} catch (IllegalArgumentException | IllegalAccessException e) {				
				//e.printStackTrace();
			}
			if(lineSeperator)
				builder.append(System.lineSeparator());
		}
		
		builder.append("}");
		return builder.toString().replaceFirst(";", "");
	}
	
	public static String toStringRec(Object o, boolean lineSeperator) {
		StringBuilder builder = new StringBuilder();
		if(o == null) {
			builder.append("null");
			return builder.toString();
		}
		
		if(lineSeperator)
			builder.append(System.lineSeparator());
		builder.append(o.getClass().getName());
		builder.append("{");
		if(lineSeperator)
			builder.append(System.lineSeparator());
		
		
		Class<?> clazz = o.getClass();
		
		while(clazz != null && !clazz.equals(Object.class)) {
			Field[] fields = clazz.getDeclaredFields();
			
			for(Field f : fields) {
				if(!f.isAccessible())
					try {
						f.setAccessible(true);
					} catch(Exception e) {
						continue;
					}
				builder.append((lineSeperator ? "" : ";") + f.getName());
				builder.append("=");
				try {
					Object val = f.get(o);
					if(val != null && val.getClass().isArray()) {
						Object[] arr = new Object[Array.getLength(val)];
						for(int i = 0; i<arr.length; i++)
							arr[i] = Array.get(val, i);
						val = Arrays.asList(arr);
					}
					builder.append(val);
				} catch (IllegalArgumentException | IllegalAccessException e) {				
					//e.printStackTrace();
				}
				if(lineSeperator)
					builder.append(System.lineSeparator());
			}
			
			clazz = clazz.getSuperclass();
		}
		
		builder.append("}");
		return builder.toString().replaceFirst(";", "");
	}
}
