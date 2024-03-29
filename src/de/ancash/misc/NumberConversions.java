package de.ancash.misc;

/**
 * Utils for casting number types to other number types
 *
 * @author Bukkit
 * @see <a href=
 *      "https://github.com/Bukkit/Bukkit/tree/master/src/main/java/org/bukkit/util/NumberConversions.java">Bukkit
 *      Source</a>
 */
public final class NumberConversions {

	NumberConversions() {
	}

	public static int floor(final double num) {
		final int floor = (int) num;
		return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
	}

	public static int ceil(final double num) {
		final int floor = (int) num;
		return floor == num ? floor : floor + (int) (~Double.doubleToRawLongBits(num) >>> 63);
	}

	public static int round(final double num) {
		return NumberConversions.floor(num + 0.5d);
	}

	public static double round(final double value, final int frac) {
		return Math.round(Math.pow(10.0, frac) * value) / Math.pow(10.0, frac);
	}

	public static double square(final double num) {
		return num * num;
	}

	public static int toInt(final Object object) {
		if (object instanceof Number) {
			return ((Number) object).intValue();
		}

		try {
			return Integer.valueOf(object.toString());
		} catch (final NumberFormatException | NullPointerException e) {
		}
		return 0;
	}

	public static float toFloat(final Object object) {
		if (object instanceof Number) {
			return ((Number) object).floatValue();
		}

		try {
			return Float.valueOf(object.toString());
		} catch (final NumberFormatException | NullPointerException e) {
		}
		return 0;
	}

	public static double toDouble(final Object object) {
		if (object instanceof Number) {
			return ((Number) object).doubleValue();
		}

		try {
			return Double.valueOf(object.toString());
		} catch (final NumberFormatException | NullPointerException e) {
		}
		return 0;
	}

	public static long toLong(final Object object) {
		if (object instanceof Number) {
			return ((Number) object).longValue();
		}

		try {
			return Long.valueOf(object.toString());
		} catch (final NumberFormatException | NullPointerException e) {
		}
		return 0;
	}

	public static short toShort(final Object object) {
		if (object instanceof Number) {
			return ((Number) object).shortValue();
		}

		try {
			return Short.valueOf(object.toString());
		} catch (final NumberFormatException | NullPointerException e) {
		}
		return 0;
	}

	public static byte toByte(final Object object) {
		if (object instanceof Number) {
			return ((Number) object).byteValue();
		}

		try {
			return Byte.valueOf(object.toString());
		} catch (final NumberFormatException | NullPointerException e) {
		}
		return 0;
	}

}
