package de.tum.in.opcua.server.core.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class ArrayUtils {

	public static byte[] concat(byte[] first, byte[] second) {
		final byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static <T> T[] concat(T[] first, T[] second) {
		final T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static <T> T[] concatAll(T[] first, T[]... rest) {
		int totalLength = first.length;
		for (final T[] array : rest) {
			totalLength += array.length;
		}
		final T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (final T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}

	@SuppressWarnings({ "unchecked" })
	public static <T> T[] toArray(List<T> list, Class<T> clazz) {
		if (list != null) {
			final T[] array = (T[]) Array.newInstance(clazz, list.size());
			list.toArray(array);
			return array;
		} else {
			return null;
		}
	}
}
