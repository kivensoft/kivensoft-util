package cn.kivensoft.util;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于Javolution(http://javolution.org/)的TextBuilder改造而来的类
 * 采用二维数组方式, 实现动态扩展无需频繁复制, 比SringBuilder好用
 *
 * @author kiven lee
 * @version 1.0
 */
final public class TextBuilder implements Appendable, CharSequence, Serializable {

	private static final long serialVersionUID = 0x700L; // Version.

	// We do a full resize (and copy) only when the capacity is less than C1.
	// For large collections, multi-dimensional arrays are employed.
	protected static final int B0 = 5; // Initial capacity in bits.
	protected static final int C0 = 1 << B0; // Initial capacity (32)
	protected static final int B1 = 10; // Low array maximum capacity in bits.
	protected static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	protected static final int M1 = C1 - 1; // Mask.
	// Resizes up to 1024 maximum (32, 64, 128, 256, 512, 1024).
	protected char[] _low = new char[C0];
	// For larger capacity use multi-dimensional array.
	protected char[][] _high = new char[1][];

	/**
	 * Holds the current length.
	 */
	protected int _length;

	/**
	 * Holds current capacity.
	 */
	protected int _capacity = C0;

	/**
	 * Creates a text builder of small initial capacity.
	 */
	public TextBuilder() {
		_high[0] = _low;
	}

	/**
	 * Creates a text builder holding the specified <code>String</code>
	 * (convenience method).
	 *
	 * @param str
	 *            the initial string content of this text builder.
	 */
	public TextBuilder(String str) {
		this();
		append(str);
	}

	/**
	 * Creates a text builder of specified initial capacity. Unless the text
	 * length exceeds the specified capacity, operations on this text builder
	 * will not allocate memory.
	 *
	 * @param capacity
	 *            the initial capacity.
	 */
	public TextBuilder(int capacity) {
		this();
		while (_capacity < capacity) _capacity <<= 1;
		if (_capacity > C1) {
			int highLen = _capacity >> B1;
			_low = new char[C1];
			_high = new char[highLen][];
			for (int i = 1; i < highLen; ++i)
				_high[i] = new char[C1];
		} else {
			_low = new char[_capacity];
		}
		_high[0] = _low;
	}

	/**
	/** 返回缓存长度 (字符数量)
	 *
	 * @return the number of characters (16-bits Unicode).
	 */
	public final int length() {
		return _length;
	}

	/** 返回缓存的utf8字节串长度 */
	public final int byteLength() {
		int count = 0;
		for (int i = 0, imax = _length; i < imax; ++i) {
			char c = _high[i >> B1][i & M1];
			if (c < 0x80) ++count;
			else if (c < 0x800) count += 2;
			else if (c < 0x10000) count += 3;
			else count += 4;
		}
		return count;
	}

	public final int capacity() {
		return _capacity;
	}

	/**
	 * 返回索引位置的字符
	 *
	 * @param index 索引位置
	 * @return 索引位置的字符
	 * @throws IndexOutOfBoundsException
	 *             if {@code(index < 0) || (index >= this.length())}
	 */
	public final char charAt(int index) {
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}

	/** 复制字符到目标字符数组中
	 *
	 * @param srcBegin this text start index.
	 * @param srcEnd this text end index (not included).
	 * @param dst the destination array to copy the data into.
	 * @param dstBegin the offset into the destination array.
	 * @throws IndexOutOfBoundsException
	 *             if {@code(srcBegin < 0) || (dstBegin < 0) || (srcBegin >
	 *             srcEnd) || (srcEnd > this.length()) || ((dstBegin + srcEnd -
	 *             srcBegin) > dst.length)}
	 */
	public final void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
		for (int i = srcBegin, j = dstBegin; i < srcEnd;) {
			char[] chars0 = _high[i >> B1];
			int i0 = i & M1;
			int x = C1 - i0, y = srcEnd - i;
			int length = x < y ? x : y;
			System.arraycopy(chars0, i0, dst, j, length);
			i += length;
			j += length;
		}
	}

	public final char[] getChars() {
		return getChars(0, _length);
	}

	public final char[] getChars(int begin, int end) {
		char[] ret = new char[end - begin];
		getChars(begin, end, ret, 0);
		return ret;
	}

	public final void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		int[] pos = {dstBegin};
		forEachBytes(srcBegin, srcEnd, (bytes, start, length) -> {
			System.arraycopy(bytes, start, dst, pos[0], length);
			pos[0] += length;
			return true;
		});
	}

	public final byte[] getBytes() {
		return getBytes(0, _length);
	}

	public final byte[] getBytes(int begin, int end) {
		byte[] ret = new byte[byteLength()];
		getBytes(begin, end, ret, 0);
		return ret;
	}

	@FunctionalInterface
	public interface onForEach {
		boolean call(char[] chars, int off, int len);
	}

	public final void forEach(onForEach act) {
		forEach(0, _length, act);
	}

	public final void forEach(int begin, int end, onForEach act) {
		for (int i = begin; i < end;) {
			char[] chars = _high[i >> B1];
			int off = i & M1;
			int x = C1 - off, y = end - i;
			int len = x < y ? x : y;
			if (!act.call(chars, off, len)) return;
			i += len;
		}
	}

	@FunctionalInterface
	public interface onForEachBytes {
		boolean call(byte[] bytes, int off, int len);
	}

	public final void forEachBytes(onForEachBytes act) {
		forEachBytes(0, _length, act);
	}

	public final void forEachBytes(int begin, int end, onForEachBytes act) {
		int bl = _length < C1 ? _length : C1;
		byte[] bytes = new byte[bl << 2];
		forEach(begin, end, (chars, off, len) -> {
			int idx = 0;
			for (int i = off, imax = off + len; i < imax; ++i)
				idx += charToUtf8(chars[i], bytes, idx);
			return act.call(bytes, 0, idx);
		});
	}

	public final static int charToUtf8(char ch, byte[] dst, int off) {
		int c = (int) ch & 0x00FF_FFFF;
		if (c < 0x80) {
			dst[++off] = (byte) c;
			return 1;
		}

		int bc;
		if (c < 0x0800)        bc = 0;
		else if (c < 0x1_0000) bc = 1;
		else                   bc = 2;

		dst[++off] = (byte) ((c >> ((bc + 1) * 6)) | ((0xF0 << (2 - bc)) & 0xF0));
		for (int j = bc * 6; j >= 0; j -= 6)
			dst[++off] = (byte) (((c >> j) & 0x3F) | 0x80);

		return bc + 2;
	}

	/**
	 * Sets the character at the specified position.
	 *
	 * @param index the index of the character to modify.
	 * @param c new character.
	 * @throws IndexOutOfBoundsException
	 *             if {@code(index < 0) || (index >= this.length())}
	 */
	public final void setCharAt(int index, char c) {
		_high[index >> B1][index & M1] = c;
	}

	/**
	 * Convenience method equivalent to {@link #setLength(int, char)
	 * setLength(newLength, '\u0000')}.
	 *
	 * @param newLength the new length of this builder.
	 * @throws IndexOutOfBoundsException if {@code(newLength < 0)}
	 */
	public final void setLength(int newLength) {
		setLength(newLength, '\u0000');
	}

	/**
	 * Sets the length of this character builder. If the length is greater than
	 * the current length; the specified character is inserted.
	 *
	 * @param newLength the new length of this builder.
	 * @param fillChar the character to be appended if required.
	 * @throws IndexOutOfBoundsException if {@code(newLength < 0)}
	 */
	public final void setLength(int newLength, char fillChar) {
		if (newLength <= _length) _length = newLength;
		else {
			while (newLength > _capacity) increaseCapacity();
			for (int i = _length; i < newLength; ++i)
				_high[i >> B1][i & M1] = fillChar;
			_length = newLength;
		}
	}

	@Override
	public final CharSequence subSequence(int start, int end) {
		return substring(start, end);
	}

	public final String substring(int start) {
		return substring(start, _length);
	}

	/**
	 * Returns a {@link java.lang.CharSequence} corresponding to the character
	 * sequence between the specified indexes.
	 *
	 * @param start the index of the first character inclusive.
	 * @param end the index of the last character exclusive.
	 * @return a character sequence.
	 * @throws IndexOutOfBoundsException
	 *             if {@code(start < 0) || (end < 0) || (start > end) || (end > this.length())}
	 */
	public final String substring(int start, int end) {
		if (end <= C1)
			return new String(_low, start, end - start);

		char[] chars = new char[end - start];
		getChars(start, end, chars, 0);
		return new String(chars);
	}

	public final void toAppendable(Appendable append) throws IOException {
		IOException[] ex = new IOException[1];
		forEach((chars0, start, len) -> {
			try {
				for (int i = start, jmax = start + len; i < jmax; ++i)
					append.append(chars0[i]);
				return true;
			} catch (IOException e) {
				ex[0] = e;
				return false;
			}
		});
		if (ex[0] != null) throw ex[0];
	}

	/**
	 * Appends the specified character.
	 *
	 * @param c the character to append.
	 * @return <code>this</code>
	 */
	public final TextBuilder append(char c) {
		if (_length >= _capacity) increaseCapacity();
		_high[_length >> B1][_length & M1] = c;
		++_length;
		return this;
	}

	protected final TextBuilder appendNull() {
		return append("null");
	}

	public static final String newLine = System.getProperty("line.separator");

	/** 获取平台相关的回车换行符
	 * @return 回车换行符
	 */
	public final String NL() {
		return newLine;
	}

	/** 添加回车换行,与系统平台相关 */
	public final TextBuilder appendNewLine() {
		append(newLine);
		return this;
	}

	/**
	 * Appends the specified character sequence. If the specified character
	 * sequence is <code>null</code> this method is equivalent to
	 * <code>append("null")</code>.
	 *
	 * @param csq the character sequence to append or <code>null</code>.
	 * @return <code>this</code>
	 */
	public final TextBuilder append(CharSequence csq) {
		return (csq == null) ? appendNull() : append(csq, 0, csq.length());
	}

	/**
	 * Appends a subsequence of the specified character sequence. If the
	 * specified character sequence is <code>null</code> this method is
	 * equivalent to <code>append("null")</code>.
	 *
	 * @param csq the character sequence to append or <code>null</code>.
	 * @param start the index of the first character to append.
	 * @param end the index after the last character to append.
	 * @return <code>this</code>
	 * @throws IndexOutOfBoundsException
	 *             if {@code(start < 0) || (end < 0) || (start > end) || (end >
	 *             csq.length())}
	 */
	public final TextBuilder append(CharSequence csq, int start, int end) {
		if (csq == null) return appendNull();
		int pos = _length, newLength = _length + end - start;
		while (newLength > _capacity) increaseCapacity();
		for (int i = start; i < end; ++i, ++pos)
			_high[pos >> B1][pos & M1] = csq.charAt(i);
		_length = newLength;
		return this;
	}

	/**
	 * Appends the specified string to this text builder. If the specified
	 * string is <code>null</code> this method is equivalent to
	 * <code>append("null")</code>.
	 *
	 * @param str the string to append or <code>null</code>.
	 * @return <code>this</code>
	 */
	public final TextBuilder append(String str) {
		return (str == null) ? appendNull() : append(str, 0, str.length());
	}

	/**
	 * Appends a subsequence of the specified string. If the specified character
	 * sequence is <code>null</code> this method is equivalent to
	 * <code>append("null")</code>.
	 *
	 * @param str the string to append or <code>null</code>.
	 * @param start the index of the first character to append.
	 * @param end the index after the last character to append.
	 * @return <code>this</code>
	 * @throws IndexOutOfBoundsException
	 *             if {@code(start < 0) || (end < 0) || (start > end) || (end >
	 *             str.length())}
	 */
	public final TextBuilder append(String str, int start, int end) {
		if (str == null) return appendNull();
		int newLength = _length + end - start;

		while (newLength > _capacity) increaseCapacity();

		for (int i = start, j = _length; i < end;) {
			char[] chars = _high[j >> B1];
			int dstBegin = j & M1;
			int x = C1 - dstBegin, y = end - i;
			int inc = x < y ? x : y;
			str.getChars(i, (i += inc), chars, dstBegin);
			j += inc;
		}
		_length = newLength;
		return this;
	}

	public final TextBuilder appendNotNull(String text) {
		return text == null || text.length() == 0 ? this : append(text);
	}

	/**
	 * Appends the characters from the char array argument.
	 *
	 * @param chars the character array source.
	 * @return <code>this</code>
	 */
	public final TextBuilder append(char chars[]) {
		append(chars, 0, chars.length);
		return this;
	}

	/**
	 * Appends the characters from a subarray of the char array argument.
	 *
	 * @param chars the character array source.
	 * @param offset the index of the first character to append.
	 * @param length the number of character to append.
	 * @return <code>this</code>
	 * @throws IndexOutOfBoundsException
	 *             if {@code(offset < 0) || (length < 0) || ((offset + length) >
	 *             chars.length)}
	 */
	public final TextBuilder append(char chars[], int offset, int length) {
		final int end = offset + length;
		int newLength = _length + length;

		while (newLength > _capacity) increaseCapacity();

		for (int i = offset, j = _length; i < end;) {
			char[] dstChars = _high[j >> B1];
			int dstBegin = j & M1;
			int x = C1 - dstBegin, y = end - i;
			int inc = x < y ? x : y;
			System.arraycopy(chars, i, dstChars, dstBegin, inc);
			i += inc;
			j += inc;
		}
		_length = newLength;
		return this;
	}

	/** 追加utf8字节数组, 转成unicode字符追加进来
	 * @param utf8_bytes
	 * @return
	 */
	public final TextBuilder append(byte[] utf8_bytes) {
		return utf8_bytes == null ? appendNull() : append(utf8_bytes, 0, utf8_bytes.length);
	}

	/** 追加utf8字节数组, 转成unicode字符追加进来
	 * @param utf8_bytes
	 * @return
	 */
	public final TextBuilder append(byte[] utf8_bytes, int off, int len) {
		resetUtf8Flag();
		return nextAppend(utf8_bytes, off, len);
	}

	private int lastUtf8Char = 0, requireUtf8Count = 0; // 上次读取结果及缺少的字节数

	public final TextBuilder resetUtf8Flag() {
		lastUtf8Char = 0;
		requireUtf8Count = 0;
		return this;
	}

	/** 追加utf8字节数组, 转成unicode字符追加进来, 适用于分段追加, 保留上次未追加完的字节到下次
	 * @param utf8_bytes
	 * @return
	 */
	public final TextBuilder nextAppend(byte[] utf8_bytes) {
		return nextAppend(utf8_bytes, 0, utf8_bytes.length);
	}

	/** 追加utf8字节数组, 转成unicode字符追加进来, 适用于分段追加, 保留上次未追加完的字节到下次
	 * @param utf8_bytes
	 * @return
	 */
	public final TextBuilder nextAppend(byte[] utf8_bytes, int off, int len) {
		if (len == 0) return this;
		int b = requireUtf8Count > 0 ? lastUtf8Char : 0, next = requireUtf8Count;
		requireUtf8Count = 0;
		int pos = off, max_pos = off + len, next_pos, imax;
		while (pos < max_pos) {
			if (next == 0) {
				b = utf8_bytes[pos++] & 0xFF;

				if (b < 0x80) {
					append((char) b);
					continue;
				}

				if (b < 0xC0) throw new RuntimeException("bad byte to transaction utf8");
				else if (b < 0xE0) next = 1;
				else if (b < 0xF0) next = 2;
				else if (b < 0xF8) next = 3;
				else if (b < 0xFC) next = 4;
				else next = 5;

				b &= 0x3F >> next;
			}

			next_pos = pos + next;
			imax = next_pos > max_pos ? max_pos : next_pos;
			for (int i = pos; i < imax; ++i)
				b = b << 6 | utf8_bytes[i] & 0x3F;
			pos = imax;
			next = 0;

			if (next_pos > max_pos) {
				lastUtf8Char = b;
				requireUtf8Count = next_pos - max_pos;
			} else {
				if (b <= 0xFFFF) append((char) b);
				else if (b <= 0xEFFFF) {
					append((char) (0xD800 + (b >> 10) - 0x40));
					append((char) (0xDC00 + (b & 0x3FF)));
				}
				else throw new RuntimeException("bad byte to transaction utf8");
			}
		}

		return this;
	}

	/**
	 * Appends the textual representation of the specified <code>boolean</code>
	 * argument.
	 *
	 * @param b the <code>boolean</code> to format.
	 * @return <code>this</code>
	 * @see TypeFormat
	 */
	public final TextBuilder append(boolean b) {
		return append(b ? "true" : "false");
	}

	/**
	 * Appends the decimal representation of the specified <code>int</code> argument.
	 *
	 * @param i the <code>int</code> to format.
	 * @return <code>this</code>
	 */
	public final TextBuilder append(int i) {
		if (i <= 0) {
			if (i == 0) return append("0");
			// Negation would overflow.
			if (i == Integer.MIN_VALUE) return append("-2147483648");
			append('-');
			i = -i;
		}
		int digits = digitLength(i);
		if (_capacity < _length + digits) increaseCapacity();
		_length += digits;

		for (int index = _length - 1;; index--) {
			int j = i / 10;
			_high[index >> B1][index & M1] = (char) ('0' + i - (j * 10));
			if (j == 0) return this;
			i = j;
		}
	}

	/**
	 * Appends the radix representation of the specified <code>int</code> argument.
	 *
	 * @param i the <code>int</code> to format.
	 * @param radix the radix (e.g. <code>16</code> for hexadecimal).
	 * @return <code>this</code>
	 */
	public final TextBuilder append(int i, int radix) {
		if (radix == 10) return append(i); // Faster.
		if (radix < 2 || radix > 36)
			throw new IllegalArgumentException("radix: " + radix);

		if (i < 0) {
			append('-');
			if (i == Integer.MIN_VALUE) { // Negative would overflow.
				appendPositive(-(i / radix), radix);
				return append(DIGIT_TO_CHAR[-(i % radix)]);
			}
			i = -i;
		}

		appendPositive(i, radix);
		return this;
	}

	private void appendPositive(int l1, int radix) {
		if (l1 >= radix) {
			int l2 = l1 / radix;
			// appendPositive(l2, radix);
			if (l2 >= radix) {
				int l3 = l2 / radix;
				// appendPositive(l3, radix);
				if (l3 >= radix) {
					int l4 = l3 / radix;
					appendPositive(l4, radix);
					append(DIGIT_TO_CHAR[l3 - (l4 * radix)]);
				} else {
					append(DIGIT_TO_CHAR[l3]);
				}
				append(DIGIT_TO_CHAR[l2 - (l3 * radix)]);
			} else {
				append(DIGIT_TO_CHAR[l2]);
			}
			append(DIGIT_TO_CHAR[l1 - (l2 * radix)]);
		} else {
			append(DIGIT_TO_CHAR[l1]);
		}
	}

	private final static char[] DIGIT_TO_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
			'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	/**
	 * Appends the decimal representation of the specified <code>long</code> argument.
	 *
	 * @param l the <code>long</code> to format.
	 * @return <code>this</code>
	 */
	public final TextBuilder append(long l) {
		if (l <= 0) {
			if (l == 0)
				return append("0");
			if (l == Long.MIN_VALUE) // Negation would overflow.
				return append("-9223372036854775808");
			append('-');
			l = -l;
		}

		if (l <= Integer.MAX_VALUE) return append((int) l);
		append(l / 1000000000);
		int i = (int) (l % 1000000000);
		int digits = digitLength(i);
		append("000000000", 0, 9 - digits);
		return append(i);
	}

	/**
	 * Appends the radix representation of the specified <code>long</code> argument.
	 *
	 * @param l the <code>long</code> to format.
	 * @param radix the radix (e.g. <code>16</code> for hexadecimal).
	 * @return <code>this</code>
	 */
	public final TextBuilder append(long l, int radix) {
		if (radix == 10) return append(l); // Faster.
		if (radix < 2 || radix > 36)
			throw new IllegalArgumentException("radix: " + radix);

		if (l < 0) {
			append('-');
			if (l == Long.MIN_VALUE) { // Negative would overflow.
				appendPositive(-(l / radix), radix);
				return append(DIGIT_TO_CHAR[(int) -(l % radix)]);
			}
			l = -l;
		}

		appendPositive(l, radix);
		return this;
	}

	private void appendPositive(long l1, int radix) {
		if (l1 >= radix) {
			long l2 = l1 / radix;
			// appendPositive(l2, radix);
			if (l2 >= radix) {
				long l3 = l2 / radix;
				// appendPositive(l3, radix);
				if (l3 >= radix) {
					long l4 = l3 / radix;
					appendPositive(l4, radix);
					append(DIGIT_TO_CHAR[(int) (l3 - (l4 * radix))]);
				} else {
					append(DIGIT_TO_CHAR[(int) l3]);
				}
				append(DIGIT_TO_CHAR[(int) (l2 - (l3 * radix))]);
			} else {
				append(DIGIT_TO_CHAR[(int) l2]);
			}
			append(DIGIT_TO_CHAR[(int) (l1 - (l2 * radix))]);
		} else {
			append(DIGIT_TO_CHAR[(int) l1]);
		}
	}

	/**
	 * Appends the textual representation of the specified <code>float</code>.
	 *
	 * @param f the <code>float</code> to format.
	 * @return <code>append(f, 10, (abs(f) &gt;= 1E7) || (abs(f) &lt; 0.001), false)</code>
	 */
	public final TextBuilder append(float f) {
		float ff = f < 0 ? -f : f;
		return append(f, 10, (ff >= 1E7) || (ff < 0.001), false);
	}

	/**
	 * Appends the textual representation of the specified <code>double</code>;
	 * the number of digits is 17 or 16 when the 16 digits representation can be
	 * parsed back to the same <code>double</code> (mimic the standard library
	 * formatting).
	 *
	 * @param d the <code>double</code> to format.
	 * @return {@code append(d, -1, (MathLib.abs(d) >= 1E7) ||
	 *        (MathLib.abs(d) < 0.001), false)}
	 */
	public final TextBuilder append(double d) {
		double dd = d < 0 ? -d : d;
		return append(d, -1, (dd >= 1E7) || (dd < 0.001), false);
	}

	/**
	 * Appends the textual representation of the specified <code>double</code>
	 * according to the specified formatting arguments.
	 *
	 * @param d the <code>double</code> value.
	 * @param digits
	 *            the number of significative digits (excludes exponent) or
	 *            <code>-1</code> to mimic the standard library (16 or 17
	 *            digits).
	 * @param scientific
	 *            <code>true</code> to forces the use of the scientific notation
	 *            (e.g. <code>1.23E3</code>); <code>false</code> otherwise.
	 * @param showZero
	 *            <code>true</code> if trailing fractional zeros are
	 *            represented; <code>false</code> otherwise.
	 * @return <code>TypeFormat.format(d, digits, scientific, showZero, this)</code>
	 * @throws IllegalArgumentException
	 *             if <code>(digits &gt; 19)</code>)
	 */
	public final TextBuilder append(double d, int digits, boolean scientific, boolean showZero) {
		if (digits > 19)
			throw new IllegalArgumentException("digits: " + digits);
		if (d != d) // NaN
			return append("NaN");
		if (d == Double.POSITIVE_INFINITY)
			return append("Infinity");
		if (d == Double.NEGATIVE_INFINITY)
			return append("-Infinity");
		if (d == 0.0) { // Zero.
			if (digits < 0)
				return append("0.0");
			append('0');
			if (showZero) {
				append('.');
				for (int j = 1; j < digits; j++) {
					append('0');
				}
			}
			return this;
		}
		if (d < 0) { // Work with positive number.
			d = -d;
			append('-');
		}

		// Find the exponent e such as: value == x.xxx * 10^e
		int e = floorLog10(d);

		long m;
		if (digits < 0) { // Use 16 or 17 digits.
			// Try 17 digits.
			long m17 = toLongPow10(d, (17 - 1) - e);
			// Check if we can use 16 digits.
			long m16 = m17 / 10;
			double dd = toDoublePow10(m16, e - 16 + 1);
			if (dd == d) { // 16 digits is enough.
				digits = 16;
				m = m16;
			} else { // We cannot remove the last digit.
				digits = 17;
				m = m17;
			}
		} else
			// Use the specified number of digits.
			m = toLongPow10(d, (digits - 1) - e);

		// Formats.
		if (scientific || (e >= digits)) {
			// Scientific notation has to be used ("x.xxxEyy").
			long pow10 = POW10_LONG[digits - 1];
			int k = (int) (m / pow10); // Single digit.
			append((char) ('0' + k));
			m = m - pow10 * k;
			appendFraction(m, digits - 1, showZero);
			append('E');
			append(e);
		} else { // Dot within the string ("xxxx.xxxxx").
			int exp = digits - e - 1;
			if (exp < POW10_LONG.length) {
				long pow10 = POW10_LONG[exp];
				long l = m / pow10;
				append(l);
				m = m - pow10 * l;
			} else
				append('0'); // Result of the division by a power of 10 larger
								// than any long.
			appendFraction(m, exp, showZero);
		}
		return this;
	}

	private void appendFraction(long l, int digits, boolean showZero) {
		append('.');
		if (l == 0)
			if (showZero)
				for (int i = 0; i < digits; i++) append('0');
			else
				append('0');
		else { // l is different from zero.
			int length = digitLength(l);
			for (int j = length; j < digits; j++)
				append('0'); // Add leading zeros.
			if (!showZero)
				while (l % 10 == 0)
					l /= 10; // Remove trailing zeros.
			append(l);
		}
	}

	public final TextBuilder appendHex(byte b) {
		int b1 = (b >> 4) & 0x0F, b2 = b & 0xF;
		return append((char) (b1 < 10 ? 48 + b1 : 87 + b1))
				.append((char) (b2 < 10 ? 48 + b2 : 87 + b2));
	}

	public final TextBuilder appendHex(final byte[] bytes, char[] delimiter) {
		if(bytes == null) return appendNull();

		final int len = bytes.length, dlen = delimiter == null ? 0 : delimiter.length;
		char[] tmp = new char[(dlen + 2) << 3];

		// 按8字节为一组进行批处理
		for (int i = 0, imax = len >> 3; i < imax; ++i) {
			for (int j = i << 3, jmax = j + 8, idx = -1; j < jmax; ++j) {
				int b = bytes[j], b1 = (b >> 4) & 0xF, b2 = b & 0xF;
				tmp[++idx] = (char) (b1 < 10 ? 48 + b1 : 87 + b1);
				tmp[++idx] = (char) (b2 < 10 ? 48 + b2 : 87 + b2);
				if (delimiter != null)
					for (int k = 0; k < dlen; ++k)
						tmp[++idx] = delimiter[k];
			}
			append(tmp);
		}

		// 处理剩余的不到8字节倍数的数据
		for (int i = len - (len & 7); i < len; ++i) {
			int b = bytes[i], b1 = (b >> 4) & 0xF, b2 = b & 0xF;
			append((char) (b1 < 10 ? 48 + b1 : 87 + b1))
					.append((char) (b2 < 10 ? 48 + b2 : 87 + b2));
			if (delimiter != null) append(delimiter);
		}
		if (delimiter != null) setLength(length() - 1);

		return this;
	}

	public final TextBuilder appendHex(final int value) {
		for (int i = 32 - 4; i >= 0; i -= 4) {
			int b = (value >> i) & 0xf;
			append((char) (b < 10 ? 48 + b : 87 + b));
		}
		return this;
	}

	public final TextBuilder appendHex(final long value) {
		for (int i = 64 - 4; i >= 0; i -= 4) {
			long b = (value >> i) & 0xf;
			append((char) (b < 10 ? 48 + b : 87 + b));
		}
		return this;
	}

	/** base64编码表 */
	private final static char[] BASE64_DIGEST = {
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
			'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
			'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
	/** base64结尾填充字符常量 */
	private final static char PAD = '=';

	/** 以base64编码方式追加字节数组
	 * @param bytes 要编码的字节数组
	 * @param lineBreak 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public final TextBuilder appendBase64(String text) {
		if (text == null) return appendNull();
		else if (text.isEmpty()) return this;
		else return appendBase64(text, false);
	}

	/** 以base64编码方式追加字节数组
	 * @param bytes 要编码的字节数组
	 * @param lineBreak 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public final TextBuilder appendBase64(String text, boolean lineBreak) {
		try {
			byte[] bs = text.getBytes("UTF8");
			return appendBase64(bs, lineBreak);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** 以base64编码方式追加字节数组
	 * @param bytes 要编码的字节数组
	 * @param lineBreak 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public final TextBuilder appendBase64(final byte[] bytes, boolean lineBreak) {
		//base64转码为3个字节转4个字节，即3个8位转成4个前两位固定为0的共24位
		if (bytes == null || bytes.length == 0) return this;
		int len = bytes.length;
		int bpos = 0, cpos = 0, cc = 0; //字符数组和字节数组的当前写入和读取的索引位置
		//转码数组长度，3的倍数乘4
		int dlen = (len + 2) / 3 * 4;
		if (lineBreak) dlen += (dlen - 1) / 76 << 1;
		for (int slen = len - 2, rdlen = dlen - 2; bpos < slen; bpos += 3) {
			int b1 = bytes[bpos] & 0xFF; //与FF是防止java的负数二进制补码扩展
			int b2 = bytes[bpos + 1] & 0xFF;
			int b3 = bytes[bpos + 2] & 0xFF;
			//1:第一字节的头6位, 2:第一字节的后2位+第二字节的前4位
			//3:第二字节的前4位+第三字节的后2位, 4:第四字节的后6位
			append(BASE64_DIGEST[b1 >>> 2])
				.append(BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F])
				.append(BASE64_DIGEST[((b2 << 2) | (b3 >>> 6)) & 0x3F])
				.append(BASE64_DIGEST[b3 & 0x3F]);
			cpos += 4;

			if (lineBreak && ++cc == 19 && cpos < rdlen) {
				append('\r').append('\n');
				cpos += 2;
				cc = 0;
			}
		}

		int modcount = bytes.length % 3;
		//非字节对齐时的处理，不足后面补=号，余数为1补2个，余数为2补1个
		if (modcount > 0) {
			int b1 = bytes[bpos++] & 0xFF;
			append(BASE64_DIGEST[b1 >>> 2]);
			if (modcount == 2) {
				int b2 = bytes[bpos++] & 0xFF;
				append(BASE64_DIGEST[((b1 << 4) | (b2 >>> 4)) & 0x3F])
					.append(BASE64_DIGEST[(b2 << 2) & 0x3F]);
			}
			else{
				append(BASE64_DIGEST[(b1 << 4) & 0x3F])
					.append(PAD); //余数为1，第三个也是=号
			}
			append(PAD); //余数为1，第三个也是=号
		}

		return this;
	}

	/** 以json字符串方式追加字符串, 自动对字符串进行json方式转义 */
	public final TextBuilder appendJsonString(CharSequence value) {
		if(value == null) appendNull();
		else {
			append('"');
			for (int i = 0, len = value.length(); i < len; ++i) {
				char c = value.charAt(i);
				int finded;
				if (c > '\\') finded = -1; // 小于需要判断的字符的最大值
				else {
					switch (c) {
						case '\b': finded = 'b'; break;
						case '\t': finded = 't'; break;
						case '\f': finded = 'f'; break;
						case '\n': finded = 'n'; break;
						case '\r': finded = 'r'; break;
						//case '\'':
						case '"':
						case '/':
						case '\\': finded = c; break;
						default: finded = -1;
					}
				}
				if (finded == -1) append(c);
				else append('\\').append((char) finded);
			}
			append('"');
		}
		return this;
	}

	protected Calendar calendar;
	
	protected final Calendar getCalendar(Date date) {
		if(calendar == null) calendar = Calendar.getInstance();
		if (date != null) calendar.setTime(date);
		return calendar;
	}

	/** 格式化日期 */
	public final TextBuilder append(Date date) {
		//format("%tF %<tT", date); //输出格式 yyyy-MM-dd HH:mm:SS %tF %<tT
		return appendDateTime(date);
	}

	/** 格式化日期 */
	public final TextBuilder appendDate(Date date) {
		//format("%tF", date); //输出格式 yyyy-MM-dd
		return appendDate(getCalendar(date));
	}

	/** 格式化日期 */
	public final TextBuilder appendTime(Date date) {
		//format("%tT", date); //输出格式 HH:mm:SS
		return appendTime(getCalendar(date));
	}

	/** 格式化日期 */
	public final TextBuilder appendDateTime(Date date) {
		//format("%tF %<tT", date); //输出格式 yyyy-MM-dd HH:mm:SS
		return append(getCalendar(date));
	}

	/** 格式化日期 */
	public final TextBuilder appendGmtDateTime(Date date) {
		Calendar cal = getCalendar(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		append(cal);
		cal.setTimeZone(TimeZone.getDefault());
		return this;
	}

	/** 格式化日期 */
	public final TextBuilder append(Calendar calendar) {
		if (calendar.getTimeZone().getRawOffset() == 0) {
			appendDate(calendar).append('T');
			appendTime(calendar).append('Z');
		}
		else {
			appendDate(calendar).append(' ');
			appendTime(calendar);
		}
		return this;
	}

	/** 格式化日期 */
	public final TextBuilder appendDate(Calendar calendar) {
		return appendDate(calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DATE));
	}

	/** 格式化年月日
	 * @param year 年, 0-9999
	 * @param month 月, 1-12
	 * @param day 日, 1-31
	 * @return
	 */
	public final TextBuilder appendDate(int year, int month, int day) {
		append(year).append('-');
		if (month < 10) append('0');
		append(month).append('-');
		if (day < 10) append('0');
		return append(day);
	}

	/** 格式化时分秒
	 * @param hour 24小时, 0-23
	 * @param minute 分钟, 0-59
	 * @param second 秒, 0-59
	 * @return
	 */
	public final TextBuilder appendTime(int hour, int minute, int second) {
		if (hour < 10) append('0');
		append(hour).append(':');
		if (minute < 10) append('0');
		append(minute).append(':');
		if (second < 10) append('0');
		return append(second);
	}

	/** 格式化Calendar对象 */
	public final TextBuilder appendTime(Calendar calendar) {
		return appendTime(calendar.get(Calendar.HOUR_OF_DAY),
				calendar.get(Calendar.MINUTE),
				calendar.get(Calendar.SECOND));
	}

	/** 格式化LocalDate对象 */
	public final TextBuilder append(LocalDate date) {
		return appendDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
	}

	/** 格式化LocalTime对象 */
	public final TextBuilder append(LocalTime time) {
		return appendTime(time.getHour(), time.getMinute(), time.getSecond());
	}

	/** 格式化LocalDatetime对象 */
	public final TextBuilder append(LocalDateTime datetime) {
		append(datetime.toLocalDate()).append(' ');
		return append(datetime.toLocalTime());
	}

	private static final long[] POW10_LONG = new long[] { 1L, 10L, 100L, 1000L, 10000L, 100000L,
			1000000L, 10000000L, 100000000L, 1000000000L, 10000000000L, 100000000000L,
			1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L,
			10000000000000000L, 100000000000000000L, 1000000000000000000L };

	/**
	 * Inserts the specified character sequence at the specified location.
	 *
	 * @param index the insertion position.
	 * @param csq the character sequence being inserted.
	 * @return <code>this</code>
	 * @throws IndexOutOfBoundsException if {@code(index < 0) || (index > this.length())}
	 */
	public final TextBuilder insert(int index, CharSequence csq) {
		final int shift = csq.length();
		int newLength = _length + shift;

		while (newLength > _capacity) increaseCapacity();

		_length = newLength;
		for (int i = _length - shift; --i >= index;)
			this.setCharAt(i + shift, this.charAt(i));

		for (int i = csq.length(); --i >= 0;)
			this.setCharAt(index + i, csq.charAt(i));

		return this;
	}

	/**
	 * Removes all the characters of this text builder (equivalent to
	 * <code>this.delete(start, this.length())</code>).
	 *
	 * @return <code>this.delete(0, this.length())</code>
	 */
	public final TextBuilder clear() {
		_length = 0;
		return this;
	}

	/**
	 * Removes the characters between the specified indices.
	 *
	 * @param start the beginning index, inclusive.
	 * @param end the ending index, exclusive.
	 * @return <code>this</code>
	 * @throws IndexOutOfBoundsException
	 *             if {@code(start < 0) || (end < 0) || (start > end) || (end >
	 *             this.length())}
	 */
	public final TextBuilder delete(int start, int end) {
		for (int i = end, j = start; i < _length;)
			this.setCharAt(j++, this.charAt(i++));
		_length -= end - start;
		return this;
	}

	/**
	 * Reverses this character sequence.
	 *
	 * @return <code>this</code>
	 */
	public final TextBuilder reverse() {
		final int n = _length - 1;
		for (int j = (n - 1) >> 1; j >= 0;) {
			char c = charAt(j);
			setCharAt(j, charAt(n - j));
			setCharAt(n - j--, c);
		}
		return this;
	}

	/**
	 * Returns the <code>String</code> representation of this
	 * {@link TextBuilder}.
	 *
	 * @return the <code>java.lang.String</code> for this text builder.
	 */
	@Override
	public String toString() {
		return (_length < C1) ? new String(_low, 0, _length) : toLargeString();
	}

	protected final String toLargeString() {
		char[] data = new char[_length];
		this.getChars(0, _length, data, 0);
		return new String(data, 0, _length);
	}

	/**
	 * Returns the hash code for this text builder.
	 *
	 * @return the hash code value.
	 */
	@Override
	public int hashCode() {
		int h = 0;
		for (int i = 0; i < _length;) h = 31 * h + charAt(i++);
		return h;
	}

	/**
	 * Compares this text builder against the specified object for equality.
	 * Returns <code>true</code> if the specified object is a text builder
	 * having the same character content.
	 *
	 * @param obj the object to compare with or <code>null</code>.
	 * @return <code>true</code> if that is a text builder with the same
	 *         character content as this text; <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TextBuilder)) return false;
		TextBuilder that = (TextBuilder) obj;
		if (this._length != that._length) return false;
		for (int i = 0; i < _length;)
			if (this.charAt(i) != that.charAt(i++)) return false;
		return true;
	}

	/**
	 * Indicates if this text builder has the same character content as the
	 * specified character sequence.
	 *
	 * @param csq the character sequence to compare with.
	 * @return <code>true</code> if the specified character sequence has the
	 *         same character content as this text; <code>false</code>
	 *         otherwise.
	 */
	public final boolean contentEquals(CharSequence csq) {
		if (csq.length() != _length) return false;
		for (int i = 0; i < _length;) {
			char c = _high[i >> B1][i & M1];
			if (csq.charAt(i++) != c) return false;
		}
		return true;
	}

	/**
	 * Increases this text builder capacity.
	 */
	protected final void increaseCapacity() {
		if (_capacity < C1) { // For small capacity, resize.
			_capacity <<= 1;
			char[] tmp = new char[_capacity];
			System.arraycopy(_low, 0, tmp, 0, _length);
			_low = tmp;
			_high[0] = tmp;
		} else { // Add a new low block of 1024 elements.
			int highIndex = _capacity >> B1;
			if (highIndex >= _high.length) { // Resizes _high.
				char[][] tmp = new char[_high.length * 2][];
				System.arraycopy(_high, 0, tmp, 0, _high.length);
				_high = tmp;
			}
			_high[highIndex] = new char[C1];
			_capacity += C1;
		}
	}

	public static final int digitLength(int i) {
		if (i >= 0)
			return (i >= 100000)
					? (i >= 10000000) ? (i >= 1000000000) ? 10 : (i >= 100000000) ? 9 : 8
							: (i >= 1000000) ? 7 : 6
					: (i >= 100) ? (i >= 10000) ? 5 : (i >= 1000) ? 4 : 3 : (i >= 10) ? 2 : 1;
		if (i == Integer.MIN_VALUE)
			return 10; // "2147483648".length()
		return digitLength(-i); // No overflow possible.
	}

	public static final int digitLength(long l) {
		if (l >= 0)
			return (l <= Integer.MAX_VALUE) ? digitLength((int) l) : // At least 10 digits or more.
					(l >= 100000000000000L)
							? (l >= 10000000000000000L)
									? (l >= 1000000000000000000L) ? 19
											: (l >= 100000000000000000L) ? 18 : 17
									: (l >= 1000000000000000L) ? 16 : 15
							: (l >= 100000000000L)
									? (l >= 10000000000000L) ? 14 : (l >= 1000000000000L) ? 13 : 12
									: (l >= 10000000000L) ? 11 : 10;
		if (l == Long.MIN_VALUE)
			return 19; // "9223372036854775808".length()
		return digitLength(-l);
	}

	private static final double LOG2_DIV_LOG10 = 0.3010299956639811952137388947;

	private static final int floorLog2(double d) {
		if (d <= 0)
			throw new ArithmeticException("Negative number or zero");
		long bits = Double.doubleToLongBits(d);
		int exp = ((int) (bits >> 52)) & 0x7FF;
		if (exp == 0x7FF)
			throw new ArithmeticException("Infinity or NaN");
		if (exp == 0)
			return floorLog2(d * 18014398509481984L) - 54; // 2^54 Exact.
		return exp - 1023;
	}

	private static final int floorLog10(double d) {
		int guess = (int) (LOG2_DIV_LOG10 * floorLog2(d));
		double pow10 = toDoublePow10(1, guess);
		if ((pow10 <= d) && (pow10 * 10 > d)) return guess;
		if (pow10 > d) return guess - 1;
		return guess + 1;
	}

	private static final double toDoublePow2(long m, int n) {
		if (m == 0)
			return 0.0;
		if (m == Long.MIN_VALUE)
			return toDoublePow2(Long.MIN_VALUE >> 1, n + 1);
		if (m < 0)
			return -toDoublePow2(-m, n);
		int bitLength = bitLength(m);
		int shift = bitLength - 53;
		long exp = 1023L + 52 + n + shift; // Use long to avoid overflow.
		if (exp >= 0x7FF)
			return Double.POSITIVE_INFINITY;
		if (exp <= 0) { // Degenerated number (subnormal, assume 0 for bit 52)
			if (exp <= -54)
				return 0.0;
			return toDoublePow2(m, n + 54) / 18014398509481984L; // 2^54 Exact.
		}
		// Normal number.
		long bits = (shift > 0) ? (m >> shift) + ((m >> (shift - 1)) & 1) : // Rounding.
				m << -shift;
		if (((bits >> 52) != 1) && (++exp >= 0x7FF))
			return Double.POSITIVE_INFINITY;
		bits &= 0x000fffffffffffffL; // Clears MSB (bit 52)
		bits |= exp << 52;
		return Double.longBitsToDouble(bits);
	}

	private static final double toDoublePow10(long m, int n) {
		if (m == 0)
			return 0.0;
		if (m == Long.MIN_VALUE)
			return toDoublePow10(Long.MIN_VALUE / 10, n + 1);
		if (m < 0)
			return -toDoublePow10(-m, n);
		if (n >= 0) { // Positive power.
			if (n > 308)
				return Double.POSITIVE_INFINITY;
			// Works with 4 x 32 bits registers (x3:x2:x1:x0)
			long x0 = 0; // 32 bits.
			long x1 = 0; // 32 bits.
			long x2 = m & MASK_32; // 32 bits.
			long x3 = m >>> 32; // 32 bits.
			int pow2 = 0;
			while (n != 0) {
				int i = (n >= POW5_INT.length) ? POW5_INT.length - 1 : n;
				int coef = POW5_INT[i]; // 31 bits max.

				if (((int) x0) != 0)
					x0 *= coef; // 63 bits max.
				if (((int) x1) != 0)
					x1 *= coef; // 63 bits max.
				x2 *= coef; // 63 bits max.
				x3 *= coef; // 63 bits max.

				x1 += x0 >>> 32;
				x0 &= MASK_32;

				x2 += x1 >>> 32;
				x1 &= MASK_32;

				x3 += x2 >>> 32;
				x2 &= MASK_32;

				// Adjusts powers.
				pow2 += i;
				n -= i;

				// Normalizes (x3 should be 32 bits max).
				long carry = x3 >>> 32;
				if (carry != 0) { // Shift.
					x0 = x1;
					x1 = x2;
					x2 = x3 & MASK_32;
					x3 = carry;
					pow2 += 32;
				}
			}

			// Merges registers to a 63 bits mantissa.
			int shift = 31 - bitLength(x3); // -1..30
			pow2 -= shift;
			long mantissa = (shift < 0) ? (x3 << 31) | (x2 >>> 1) : // x3 is 32 bits.
					(((x3 << 32) | x2) << shift) | (x1 >>> (32 - shift));
			return toDoublePow2(mantissa, pow2);

		} else { // n < 0
			if (n < -324 - 20)
				return 0.0;

			// Works with x1:x0 126 bits register.
			long x1 = m; // 63 bits.
			long x0 = 0; // 63 bits.
			int pow2 = 0;
			while (true) {

				// Normalizes x1:x0
				int shift = 63 - bitLength(x1);
				x1 <<= shift;
				x1 |= x0 >>> (63 - shift);
				x0 = (x0 << shift) & MASK_63;
				pow2 -= shift;

				// Checks if division has to be performed.
				if (n == 0)
					break; // Done.

				// Retrieves power of 5 divisor.
				int i = (-n >= POW5_INT.length) ? POW5_INT.length - 1 : -n;
				int divisor = POW5_INT[i];

				// Performs the division (126 bits by 31 bits).
				long wh = (x1 >>> 32);
				long qh = wh / divisor;
				long r = wh - qh * divisor;
				long wl = (r << 32) | (x1 & MASK_32);
				long ql = wl / divisor;
				r = wl - ql * divisor;
				x1 = (qh << 32) | ql;

				wh = (r << 31) | (x0 >>> 32);
				qh = wh / divisor;
				r = wh - qh * divisor;
				wl = (r << 32) | (x0 & MASK_32);
				ql = wl / divisor;
				x0 = (qh << 32) | ql;

				// Adjusts powers.
				n += i;
				pow2 -= i;
			}
			return toDoublePow2(x1, pow2);
		}
	}

	private static final long MASK_63 = 0x7FFFFFFFFFFFFFFFL;

	private static final long MASK_32 = 0xFFFFFFFFL;

	private static final int[] POW5_INT = { 1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125,
			9765625, 48828125, 244140625, 1220703125 };

	private static final int bitLength(long l) {
		if (l < 0)
			l = -(l + 1);
		return 64 - numberOfLeadingZeros(l);
	}

	private static final int numberOfLeadingZeros(long unsigned) { // From Hacker's Delight
		if (unsigned == 0)
			return 64;
		int n = 1, x = (int) (unsigned >>> 32);
		if (x == 0) {
			n += 32;
			x = (int) unsigned;
		}
		if (x >>> 16 == 0) {
			n += 16;
			x <<= 16;
		}
		if (x >>> 24 == 0) {
			n += 8;
			x <<= 8;
		}
		if (x >>> 28 == 0) {
			n += 4;
			x <<= 4;
		}
		if (x >>> 30 == 0) {
			n += 2;
			x <<= 2;
		}
		n -= x >>> 31;
		return n;
	}

	private static final long toLongPow10(double d, int n) {
		long bits = Double.doubleToLongBits(d);
		boolean isNegative = (bits >> 63) != 0;
		int exp = ((int) (bits >> 52)) & 0x7FF;
		long m = bits & 0x000fffffffffffffL;
		if (exp == 0x7FF)
			throw new ArithmeticException("Cannot convert to long (Infinity or NaN)");
		if (exp == 0) {
			if (m == 0)
				return 0L;
			return toLongPow10(d * 1E16, n - 16);
		}
		m |= 0x0010000000000000L; // Sets MSB (bit 52)
		int pow2 = exp - 1023 - 52;
		// Retrieves 63 bits m with n == 0.
		if (n >= 0) {
			// Works with 4 x 32 bits registers (x3:x2:x1:x0)
			long x0 = 0; // 32 bits.
			long x1 = 0; // 32 bits.
			long x2 = m & MASK_32; // 32 bits.
			long x3 = m >>> 32; // 32 bits.
			while (n != 0) {
				int i = (n >= POW5_INT.length) ? POW5_INT.length - 1 : n;
				int coef = POW5_INT[i]; // 31 bits max.

				if (((int) x0) != 0)
					x0 *= coef; // 63 bits max.
				if (((int) x1) != 0)
					x1 *= coef; // 63 bits max.
				x2 *= coef; // 63 bits max.
				x3 *= coef; // 63 bits max.

				x1 += x0 >>> 32;
				x0 &= MASK_32;

				x2 += x1 >>> 32;
				x1 &= MASK_32;

				x3 += x2 >>> 32;
				x2 &= MASK_32;

				// Adjusts powers.
				pow2 += i;
				n -= i;

				// Normalizes (x3 should be 32 bits max).
				long carry = x3 >>> 32;
				if (carry != 0) { // Shift.
					x0 = x1;
					x1 = x2;
					x2 = x3 & MASK_32;
					x3 = carry;
					pow2 += 32;
				}
			}

			// Merges registers to a 63 bits mantissa.
			int shift = 31 - bitLength(x3); // -1..30
			pow2 -= shift;
			m = (shift < 0) ? (x3 << 31) | (x2 >>> 1) : // x3 is 32 bits.
					(((x3 << 32) | x2) << shift) | (x1 >>> (32 - shift));

		} else { // n < 0

			// Works with x1:x0 126 bits register.
			long x1 = m; // 63 bits.
			long x0 = 0; // 63 bits.
			while (true) {

				// Normalizes x1:x0
				int shift = 63 - bitLength(x1);
				x1 <<= shift;
				x1 |= x0 >>> (63 - shift);
				x0 = (x0 << shift) & MASK_63;
				pow2 -= shift;

				// Checks if division has to be performed.
				if (n == 0)
					break; // Done.

				// Retrieves power of 5 divisor.
				int i = (-n >= POW5_INT.length) ? POW5_INT.length - 1 : -n;
				int divisor = POW5_INT[i];

				// Performs the division (126 bits by 31 bits).
				long wh = (x1 >>> 32);
				long qh = wh / divisor;
				long r = wh - qh * divisor;
				long wl = (r << 32) | (x1 & MASK_32);
				long ql = wl / divisor;
				r = wl - ql * divisor;
				x1 = (qh << 32) | ql;

				wh = (r << 31) | (x0 >>> 32);
				qh = wh / divisor;
				r = wh - qh * divisor;
				wl = (r << 32) | (x0 & MASK_32);
				ql = wl / divisor;
				x0 = (qh << 32) | ql;

				// Adjusts powers.
				n += i;
				pow2 -= i;
			}
			m = x1;
		}
		if (pow2 > 0)
			throw new ArithmeticException("Overflow");
		if (pow2 < -63)
			return 0;
		m = (m >> -pow2) + ((m >> -(pow2 + 1)) & 1); // Rounding.
		return isNegative ? -m : m;
	}

}
