package cn.kivensoft.util;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;

/**格式化到动态字符串缓冲区的类，采取缓存方式实现，增强对日期、数组、列表的格式化
 * @author Kiven Lee
 * @version 2.0
 */
final public class Fmt implements Appendable, CharSequence {
	// 全局无锁非阻塞堆栈头部指针
	private static AtomicReference<WeakReference<Fmt>> head = new AtomicReference<>();
	private final WeakReference<Fmt> self;
	private WeakReference<Fmt> next;

	protected TextBuilder buffer;

	// 无锁非阻塞弹出栈顶元素
	private final static Fmt pop() {
		WeakReference<Fmt> top, next;
		Fmt item;
		do {
			top = head.get();
			if (top == null) return null;
			item = top.get();
			if (item == null) {
				head.compareAndSet(top, null);
				return null;
			}
			next = item.next;
		} while (!head.compareAndSet(top, next));
		item.next = null;
		return item;
	}

	// 无锁非阻塞元素压入栈顶
	private final static void push(Fmt value) {
		if (value.next != null) return;
		value.buffer.setLength(0);
		WeakReference<Fmt> top;
		do {
			top = head.get();
			if (top != null && top.get() != null)
				value.next = top;
		} while (!head.compareAndSet(top, value.self));
	}

	private Fmt() {
		super();
		buffer = new TextBuilder();
		self = new WeakReference<>(this);
	}

	//静态公共函数-------------------------------------------------------------

	/** 获取缓存中的Fmt实例 */
	public static final Fmt get() {
		Fmt f = pop();
		return f != null ? f : new Fmt();
	}

	public TextBuilder getBuffer() {
		return buffer;
	}

	//回收对象
	public final void recycle() {
		push(this);
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param arg 格式化参数
	 * @return
	 */
	public static final String fmt(Object arg) {
		return get().append(arg).release();
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param arg1 格式化参数1
	 * @return
	 */
	public static final String fmt(String format, Object arg1) {
		return get().format(format, 1, arg1, null, null).release();
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @return
	 */
	public static final String fmt(String format, Object arg1, Object arg2) {
		return get().format(format, 2, arg1, arg2, null).release();
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @param arg3 格式化参数3
	 * @return
	 */
	public static final String fmt(String format, Object arg1, Object arg2, Object arg3) {
		return get().format(format, 3, arg1, arg2, arg3).release();
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 * @return
	 */
	public static final String fmt(String format, Object... args) {
		return get().format(format, args).release();
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param func 返回格式化参数的lambda表达式
	 * @return
	 */
	public static final String fmt(String format, ObjIntConsumer<Fmt> func) {
		return get().format(format, func).release();
	}

	/** 以{}为格式化标识符进行快速格式化, null的对象跳过而不是格式化为"null"
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 * @return
	 */
	public static final String fmtString(String format, Object... args) {
		for (int i = 0, imax = args.length; i < imax; ++i)
			if (args[i] == null) args[i] = "";
		return fmt(format, args);
	}

	/** 输出到控制台
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 */
	public static final void pl(String format, Object... args) {
		System.out.println(get().format(format, args).release());
	}

	/** 以{}为格式化标识符进行快速格式化，类似日志输出
	 * @param format 格式化字符串
	 * @param args 格式化参数
	 * @return
	 */
	public static final String fmtJson(String format, Object... args) {
		return get().formatJson(format, args).release();
	}

	/** 返回json化后的字符串，用缓冲区对象进行
	 * @param value 要json化的参数
	 * @return
	 */
	public static final String toJson(Object value) {
		return get().appendJson(value).release();
	}

	/** 连接2个字符串成1个 */
	public static final String concat(String arg1, String arg2) {
		Fmt f = get();
		f.buffer.append(arg1).append(arg2);
		return f.release();
	}

	/** 连接3个字符串成1个 */
	public static final String concat(String arg1, String arg2, String arg3) {
		Fmt f = get();
		f.buffer.append(arg1).append(arg2).append(arg3);
		return f.release();
	}

	/** 连接多个字符串成1个 */
	public static final String concat(String... args) {
		Fmt f = get();
		TextBuilder sb = f.buffer;
		for (int i = 0, n = args.length; i < n; ++i)
			sb.append(args[i]);
		return f.release();
	}

	/** 连接多个对象成1个 */
	public static final String concat(Object... args) {
		Fmt f = get();
		for (int i = 0, n = args.length; i < n; ++i)
			f.append(args[i]);
		return f.release();
	}

	/** 生成指定重复数量的字符串
	 * @param c 指定重复的字符
	 * @param count 重复数量
	 * @return 字符串结果
	 */
	public static final String rep(char c, int count) {
		return Fmt.get().repeat(c, count).release();
	}

	/** 生成指定重复次数的字符串
	 * @param text 指定要重复的字符串
	 * @param count 重复数量
	 * @return 生成的字符串
	 */
	public static final String rep(String text, int count) {
		return get().repeat(text, count, null).release();
	}

	/** 返回格式化后的字符串，用缓冲区对象进行
	 * @param array 要格式化的数组
	 * @param delimiter 分隔符
	 * @return
	 */
	public static final <T> String join(T[] array, String delimiter) {
		return get().append(array, delimiter, null, null, null).release();
	}

	/** 返回格式化后的字符串，用缓冲区对象进行
	 * @param array 要格式化的数组
	 * @param delimiter 分隔符
	 * @param func lamdba表达式,参数是数组的每个对象,返回Object
	 * @return
	 */
	public static final <T, R> String join(T[] array, String delimiter, Function<T, R> func) {
		return get().append(array, delimiter, null, null, func).release();
	}

	/** 返回格式化后的字符串
	 * @param iterable 要格式化的参数
	 * @param delimiter 分隔符
	 * @param func lamdba表达式,参数是数组的每个对象,返回Object
	 * @return
	 */
	public static final <T, R> String join(Iterable<T> iterable, String delimiter, Function<T, R> func) {
		return get().append(iterable, delimiter, null, null, func).release();
	}

	/** 返回格式化后的字符串
	 * @param iterable 要格式化的参数
	 * @param delimiter 分隔符
	 * @return
	 */
	public static final <T> String join(Iterable<T> iterable, String delimiter) {
		return get().append(iterable, delimiter, null, null, null).release();
	}

	/** 连接多个字符串合成的路径 */
	public static final String concatPaths(String... paths) {
		Fmt f = Fmt.get();
		for (int i = 0, imax = paths.length; i < imax; ++i)
			f.appendPath(paths[i]);
		return f.release();
	}

	/** json化字符串并输出到控制台
	 * @param fmt 格式化样式
	 * @param args 格式化参数
	 */
	public static final void printJson(String fmt, Object... args) {
		System.out.println(Fmt.fmtJson(fmt, args));
	}

	@Override
	public final int length() {
		return buffer.length();
	}

	public final int bytesLength() {
		return buffer.byteLength();
	}

	@Override
	public final char charAt(int index) {
		return buffer.charAt(index);
	}

	public final void setCharAt(int index, char c) {
		buffer.setCharAt(index, c);
	}

	@Override
	public final CharSequence subSequence(int start, int end) {
		return buffer.substring(start, end);
	}

	@Override
	public final Fmt append(CharSequence csq) {
		buffer.append(csq);
		return this;
	}

	@Override
	public final Fmt append(CharSequence csq, int start, int end) {
		buffer.append(csq, start, end);
		return this;
	}

	@Override
	public final Fmt append(char c) {
		buffer.append(c);
		return this;
	}

	public final Fmt append(boolean b) {
		buffer.append(b);
		return this;
	}

	public final Fmt append(int i) {
		buffer.append(i);
		return this;
	}

	public final Fmt append(long l) {
		buffer.append(l);
		return this;
	}

	public final Fmt append(float f) {
		buffer.append(f);
		return this;
	}

	public final Fmt append(double d) {
		buffer.append(d);
		return this;
	}

	public final Fmt append(String s) {
		buffer.append(s);
		return this;
	}

	public final Fmt append(char[] str) {
		if (str == null) buffer.appendNull();
		else buffer.append(str, 0, str.length);
		return this;
	}

	public final Fmt append(char[] str, int offset, int len) {
		if (str == null) buffer.appendNull();
		else buffer.append(str, offset, len);
		return this;
	}

	public final Fmt append(byte[] bytes) {
		buffer.append(bytes);
		return this;
	}

	public final Fmt append(byte[] bytes, int offset, int len) {
		buffer.append(bytes, offset, len);
		return this;
	}

	public final Fmt setLength(int newLength) {
		buffer.setLength(newLength);
		return this;
	}

	public final Fmt setLength(int newLength, char fillChar) {
		buffer.setLength(newLength, fillChar);
		return this;
	}

	public final String substring(int start) {
		return buffer.substring(start);
	}

	public final String substring(int start, int end) {
		return buffer.substring(start, end);
	}

	@Override
	public final String toString() {
		return buffer.toString();
	}

	/** 将字符串内容转成UTF-8字节数组返回
	 * @return 返回的字节数组
	 */
	public final byte[] getBytes() {
		return buffer.getBytes();
	}

	/** 将字符串内容转成UTF-8字节数组返回
	 * @param begin 起始位置
	 * @param end 结束位置
	 * @return 返回的字节数组
	 */
	public final byte[] getBytes(int begin, int end) {
		return buffer.getBytes(begin, end);
	}

	public final void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		buffer.getBytes(srcBegin, srcEnd, dst, dstBegin);
	}

	public final char[] getChars() {
		return buffer.getChars();
	}

	public final char[] getChars(int begin, int end) {
		return buffer.getChars(begin, end);
	}

	public final void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
		buffer.getChars(srcBegin, srcEnd, dst, dstBegin);
	}

	public final void forEach(TextBuilder.onForEach func) {
		buffer.forEach(func);
	}

	public final void forEach(int begin, int end, TextBuilder.onForEach act) {
		buffer.forEach(begin, end, act);
	}

	public final void forEachBytes(TextBuilder.onForEachBytes act) {
		buffer.forEachBytes(act);
	}

	public final void forEachBytes(int begin, int end, TextBuilder.onForEachBytes act) {
		buffer.forEachBytes(begin, end, act);
	}

	/** 回收对象，返回对象生成的字符串 */
	public final String release() {
		String ret = buffer.toString();
		recycle();
		return ret;
	}

	/** 回收对象，返回对象生成的字符串 */
	public final String release(int start) {
		return release(start, buffer.length());
	}

	/** 回收对象，返回对象生成的字符串 */
	public final String release(int start, int end) {
		String ret = buffer.substring(start, end);
		recycle();
		return ret;
	}

	/** 使用{}作为格式化参数进行格式化 */
	public final Fmt format(String format, Object... args) {
		int len = args.length;
		if (len == 0) buffer.append(format);
		else format(format, (f, i) -> f.append(i < len ? args[i] : "{}"));
		return this;
	}

	/** 使用{}作为格式化参数进行格式化
	 * @param format 字符串格式模板
	 * @param arg1 格式化参数1
	 * @param arg3 格式化参数3
	 * @return
	 */
	public final Fmt format(String format, Object arg1) {
		return format(format, 1, arg1, null, null);
	}

	/** 使用{}作为格式化参数进行格式化
	 * @param format 字符串格式模板
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @return
	 */
	public final Fmt format(String format, Object arg1, Object arg2) {
		return format(format, 2, arg1, arg2, null);
	}

	/** 使用{}作为格式化参数进行格式化
	 * @param format 字符串格式模板
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @param arg3 格式化参数3
	 * @return
	 */
	public final Fmt format(String format, Object arg1, Object arg2, Object arg3) {
		return format(format, 3, arg1, arg2, arg3);
	}

	/** 使用{}作为格式化参数进行格式化
	 * @param format 字符串格式模板
	 * @param count 格式化参数个数
	 * @param arg1 格式化参数1
	 * @param arg2 格式化参数2
	 * @param arg3 格式化参数3
	 * @return
	 */
	protected final Fmt format(String format, int count, Object arg1, Object arg2, Object arg3) {
		return format(format, (f, i) -> {
			Object ret;
			if (i < count) {
				if (i == 0) ret = arg1;
				else if (i == 1) ret = arg2;
				else if (i == 2) ret = arg3;
				else ret = "{}";
			}
			else ret = "{}";
			f.append(ret);
		});
	}

	/** 使用{}作为格式化参数进行格式化 */
	public final Fmt format(String format, IntFunction<Object> func) {
		return format(format, (f, i) -> f.append(func.apply(i)));
	}

	/** 使用{}作为格式化参数进行格式化 */
	public final Fmt format(String format, ObjIntConsumer<Fmt> func) {
		char[] fmt_chars = format.toCharArray();
		for (int i = 0, len = fmt_chars.length, idx = 0; i < len; ++i) {
			// 查找左括号出现的位置
			int old_start = i;
			for (; i < len; ++i)
				if (fmt_chars[i] == '{')
					break;

			// 先把左括号前的字符添加到输出流
			buffer.append(fmt_chars, old_start, i - old_start);
			if (i < len) {
				// 找到的左括号前导字符是反斜杠, 表明是转义字符
				if (i > 0 && fmt_chars[i - 1] == '\\')
					buffer.setLength(buffer.length() - 1);
				// 找到的左括号后续字符是右括号, 是有效的占位符, 返回跳过右括号的索引位置
				else if (i < len - 1 && fmt_chars[i + 1] == '}') {
					func.accept(this, idx++);
					++i;
					continue;
				}
				buffer.append('{');
			}
		}
		return this;
	}

	/** 使用{}作为格式化参数进行格式化 */
	public final Fmt formatJson(String format, Object... args) {
		int len = args.length;
		if (len == 0) append(format);
		else format(format, (f, i) -> f.appendJson(i < len ? args[i] : "{}"));
		return this;
	}

	/** 使用{}作为格式化参数进行格式化 */
	public final Fmt formatJson(String format, IntFunction<Object> func) {
		return format(format, (f, i) -> f.appendJson(func.apply(i)));
	}

	/** 获取平台相关的回车换行符
	 * @return 回车换行符
	 */
	public final String NL() {
		return buffer.NL();
	}

	/** 添加回车换行,与系统平台相关 */
	public final Fmt appendNewLine() {
		buffer.appendNewLine();
		return this;
	}

	public final Fmt appendNull() {
		buffer.appendNull();
		return this;
	}

	/** 对象内容追加进缓冲区, 函数自动判断大部分系统自带类型进行追加
	 * @param obj 要追加内容的对象实例
	 * @return
	 */
	public final Fmt append(Object obj) {
		if (obj == null) {
			buffer.appendNull();
			return this;
		}

		Class<?> cls = obj.getClass();
		boolean isMatch = true;
		switch (cls.getName()) {
			case "java.lang.String":
				buffer.append((String)obj);
				break;
			case "java.lang.Integer":
				buffer.append(((Integer)obj).intValue());
				break;
			case "java.lang.Long":
				buffer.append(((Long)obj).longValue());
				break;
			case "java.lang.Byte":
				buffer.append(((Byte)obj).intValue());
				break;
			case "java.lang.Short":
				buffer.append(((Short)obj).intValue());
				break;
			case "java.lang.Character":
				buffer.append(((Character)obj).charValue());
				break;
			case "java.lang.Boolean":
				buffer.append(((Boolean)obj).booleanValue());
				break;
			case "java.lang.Double":
				buffer.append(((Double)obj).doubleValue());
				break;
			case "java.lang.Float":
				buffer.append(((Float)obj).floatValue());
				break;
			case "java.util.Date":
				appendDateTime((Date)obj);
				break;
			case "java.util.ArrayList":
				append((ArrayList<?>)obj, ",", null, null, null);
				break;
			case "java.util.HashMap":
				append((Map<?, ?>)obj, ",", ":", null, null, null);
				break;
			case "java.util.GregorianCalendar":
				append((Calendar)obj);
				break;
			case "java.time.LocalDate":
				append((LocalDate)obj);
				break;
			case "java.time.LocalTime":
				append((LocalTime)obj);
				break;
			case "java.time.LocalDateTime":
				append((LocalDateTime)obj);
				break;
			case "cn.kivensoft.util.Fmt":
				buffer.append(((Fmt) obj).buffer);
				break;
			default:
				isMatch = false;
				break;
		}
		if (isMatch) return this;

		if (cls.isArray()) {
			if (cls.getComponentType() == char.class)
				buffer.append((char[]) obj);
			else if (cls.getComponentType() == byte.class)
				append((byte[]) obj);
			else if (cls.getComponentType().isPrimitive())
				appendPrimitiveArray(obj, ",", null, null);
			else append((Object[])obj, ",", null, null, null);
		}
		else if (obj instanceof CharSequence)
			buffer.append((CharSequence)obj);
		else if (obj instanceof Iterable)
			append((Iterable<?>)obj, ",", null, null, null);
		else if (obj instanceof Map)
			append((Map<?, ?>)obj, ",", ":", null, null, null);
		else if (cls.isEnum())
			buffer.append(((Enum<?>)obj).toString());
		else if (obj instanceof Calendar)
			append((Calendar)obj);
		else
			buffer.append(obj.toString());

		return this;
	}

	/** 格式化字节流, 以字符串方式写入
	 * @param bytes 字节流
	 * @param offset 起始偏移
	 * @param len 长度
	 * @param charsetName 编码名称
	 * @return
	 */
	public final Fmt appendBytes(byte[] bytes, int offset, int len, String charsetName) {
		try {
			append(new String(bytes, offset, len, charsetName));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public final Fmt append(Fmt f) {
		buffer.append(f.buffer, 0, f.buffer.length());
		return this;
	}

	public final Fmt append(Fmt f, int start, int end) {
		buffer.append(f.buffer, start, end);
		return this;
	}

	/** 格式化日期 */
	public final Fmt append(Date date) {
		buffer.appendDateTime(date);
		return this;
	}

	/** 格式化日期 */
	public final Fmt appendDate(Date date) {
		buffer.appendDate(date);
		return this;
	}

	/** 格式化日期 */
	public final Fmt appendTime(Date date) {
		appendTime(date);
		return this;
	}

	/** 格式化日期 */
	public final Fmt appendDateTime(Date date) {
		append(date);
		return this;
	}

	/** 格式化日期 */
	public final Fmt appendGmtDateTime(Date date) {
		buffer.appendGmtDateTime(date);
		return this;
	}

	/** 格式化日期 */
	public final Fmt append(Calendar calendar) {
		buffer.append(calendar);
		return this;
	}

	/** 格式化日期 */
	public final Fmt appendDate(Calendar calendar) {
		buffer.appendDate(calendar);
		return this;
	}

	/** 格式化年月日
	 * @param year 年, 0-9999
	 * @param month 月, 1-12
	 * @param day 日, 1-31
	 * @return
	 */
	public final Fmt appendDate(int year, int month, int day) {
		buffer.appendDate(year, month, day);
		return this;
	}

	/** 格式化时分秒
	 * @param hour 24小时, 0-23
	 * @param minute 分钟, 0-59
	 * @param second 秒, 0-59
	 * @return
	 */
	public final Fmt appendTime(int hour, int minute, int second) {
		buffer.appendTime(hour, minute, second);
		return this;
	}

	/** 格式化Calendar对象 */
	public final Fmt appendTime(Calendar calendar) {
		buffer.appendTime(calendar);
		return this;
	}

	/** 格式化LocalDate对象 */
	public final Fmt append(LocalDate date) {
		buffer.append(date);
		return this;
	}

	/** 格式化LocalTime对象 */
	public final Fmt append(LocalTime time) {
		buffer.append(time);
		return this;
	}

	/** 格式化LocalDatetime对象 */
	public final Fmt append(LocalDateTime datetime) {
		buffer.append(datetime);
		return this;
	}

	private static final String[] PRIMITIVE_TYPES = {
			"boolean", "byte", "char", "double", "float",
			"int", "long", "short" };

	protected static final int getPrimitiveTypeIndex(String primitiveTypeName) {
		int index = -1;
		for (int i = 0, imax = PRIMITIVE_TYPES.length; i < imax; ++i) {
			if (PRIMITIVE_TYPES[i].equals(primitiveTypeName)) {
				index = i;
				break;
			}
		}
		return index;
	}

	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param prefix 格式化前缀
	 * @param suffix 格式化后缀
	 * @param func lambda表达式，为null时使用数组本身值
	 * @return
	 */
	public final Fmt appendPrimitiveArray(Object obj, String delimiter, String prefix, String suffix) {
		if (obj == null) return appendNull();
		if (prefix != null) buffer.append(prefix);
		// 获取基本类型索引
		int index = getPrimitiveTypeIndex(obj.getClass().getComponentType().getName());
		int len = Array.getLength(obj);

		switch (index) {
			case 0:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getBoolean(obj, i)).appendNotNull(delimiter);
				break;
			case 1:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getByte(obj, i)).appendNotNull(delimiter);
				break;
			case 2:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getChar(obj, i)).appendNotNull(delimiter);
				break;
			case 3:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getDouble(obj, i)).appendNotNull(delimiter);
				break;
			case 4:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getFloat(obj, i)).appendNotNull(delimiter);
				break;
			case 5:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getInt(obj, i)).appendNotNull(delimiter);
				break;
			case 6:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getLong(obj, i)).appendNotNull(delimiter);
				break;
			case 7:
				for (int i = 0; i < len; ++i)
					buffer.append(Array.getShort(obj, i)).appendNotNull(delimiter);
				break;
			default:
				for (int i = 0; i < len; ++i)
					append(Array.get(obj, i)).appendNotNull(delimiter);
		}

		if (delimiter != null && delimiter.length() > 0)
			buffer.setLength(buffer.length() - delimiter.length());

		if (suffix != null) append(suffix);

		return this;
	}

	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @return
	 */
	public final <T> Fmt append(T[] value, String delimiter) {
		return append(value, delimiter, null, null, null);
	}

	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param prefix 格式化前缀
	 * @param suffix 格式化后缀
	 * @return
	 */
	public final <T> Fmt append(T[] value, String delimiter, String prefix,
			String suffix) {
		return append(value, delimiter, prefix, suffix, null);
	}

	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param func lambda表达式，为null时使用数组本身值
	 * @return
	 */
	public final <T, R> Fmt append(T[] value, String delimiter, Function<T, R> func) {
		return append(value, delimiter, null, null, func);
	}

	/** 格式化数组
	 * @param value 数组
	 * @param delimiter 分隔符
	 * @param prefix 格式化前缀
	 * @param suffix 格式化后缀
	 * @param func lambda表达式，为null时使用数组本身值
	 * @return
	 */
	public final <T, R> Fmt append(T[] value, String delimiter, String prefix,
			String suffix, Function<T, R> func) {
		if (value == null) buffer.appendNull();
		else {
			if (prefix != null) buffer.append(prefix);
			int len = value.length;
			if (len > 0) append(func == null ? value[0] : func.apply(value[0]));
			if (func == null) {
				if (delimiter != null) {
					for (int i = 1; i < len; ++i) {
						buffer.append(delimiter);
						append(value[i]);
					}
				}
				else for (int i = 1; i < len; ++i) append(value[i]);
			}
			else {
				if (delimiter != null) {
					for (int i = 1; i < len; ++i) {
						buffer.append(delimiter);
						append(func.apply(value[i]));
					}
				}
				else for (int i = 1; i < len; ++i) append(func.apply(value[i]));
			}
			if (suffix != null) buffer.append(suffix);
		}
		return this;
	}

	/** 格式化可迭代对象
	 * @param value 可迭代对象
	 * @param delimiter 字符串分隔符
	 * @return
	 */
	public final <T> Fmt append(Iterable<T> value, String delimiter) {
		return append(value, delimiter, null, null, null);
	}

	/** 格式化可迭代对象
	 * @param value 可迭代对象
	 * @param delimiter 字符串分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @return
	 */
	public final <T> Fmt append(Iterable<T> value, String delimiter, String prefix, String suffix) {
		return append(value, delimiter, prefix, suffix, null);
	}

	/** 格式化可迭代对象
	 * @param value 可迭代对象
	 * @param delimiter 字符串分隔符
	 * @param func 每个可迭代项的回调处理函数
	 * @return
	 */
	public final <T, R> Fmt append(Iterable<T> value, String delimiter, Function<T, R> func) {
		return append(value, delimiter, null, null, func);
	}

	/** 格式化列表
	 * @param value 列表
	 * @param delimiter 分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @param func lambda表达式，为null时使用列表本身值
	 * @return this
	 */
	public final <T, R> Fmt append(Iterable<T> value, String delimiter, String prefix,
			String suffix, Function<T, R> func) {
		Iterator<T> iter = value.iterator();
		if (iter == null) {
			buffer.appendNull();
			return this;
		}

		if (prefix != null) buffer.append(prefix);
		if (iter.hasNext())
			append(func == null ? iter.next() : func.apply(iter.next()));
		if (func == null) {
			if (delimiter != null) {
				while(iter.hasNext()) {
					buffer.append(delimiter);
					append(iter.next());
				}
			}
			else while(iter.hasNext()) append(iter.next());
		}
		else {
			if (delimiter != null) {
				while(iter.hasNext()) {
					buffer.append(delimiter);
					append(func.apply(iter.next()));
				}
			}
			else while(iter.hasNext()) append(func.apply(iter.next()));
		}
		if (suffix != null) buffer.append(suffix);
		return this;
	}

	public final <T> Fmt append(ArrayList<T> value, String delimiter) {
		return append(value, delimiter, null, null, null);
	}

	public final <T> Fmt append(ArrayList<T> value, String delimiter, String prefix,
			String suffix) {
		return append(value, delimiter, prefix, suffix, null);
	}

	/** 格式化列表
	 * @param value 列表
	 * @param delimiter 分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @param func lambda表达式，为null时使用列表本身值
	 * @return this
	 */
	public final <T> Fmt append(ArrayList<T> value, String delimiter, String prefix,
			String suffix, Function<T, Object> func) {
		if (value == null) {
			buffer.appendNull();
			return this;
		}

		if (prefix != null) buffer.append(prefix);

		for (int i = 0, imax = value.size(); i < imax; ++i) {
			if (func == null) append(value.get(i));
			else append(func.apply(value.get(0)));
			if (delimiter != null) buffer.append(delimiter);
		}
		if (delimiter != null && value.size() > 0)
			setLength(buffer.length() - delimiter.length());

		if (suffix != null) buffer.append(suffix);

		return this;
	}

	public final <K, V> Fmt append(Map<K, V> value, String delimiter1, String delimiter2) {
		return append(value, delimiter1, delimiter2, null, null, null);
	}

	public final <K, V> Fmt append(Map<K, V> value, String delimiter1, String delimiter2,
			String prefix, String suffix) {
		return append(value, delimiter1, delimiter2, prefix, suffix, null);
	}

	/** 格式化列表
	 * @param value 列表
	 * @param delimiter 分隔符
	 * @param prefix 前缀字符串
	 * @param suffix 后缀字符串
	 * @param func lambda表达式，为null时使用列表本身值
	 * @return this
	 */
	public final <K, V> Fmt append(Map<K, V> value, String delimiter1, String delimiter2,
			String prefix, String suffix, BiFunction<K, V, Object> func) {
		if (value == null) {
			buffer.appendNull();
			return this;
		}

		if (prefix != null) buffer.append(prefix);

		for (Map.Entry<K, V> entry : value.entrySet()) {
			if (func == null) {
				append(entry.getKey()).append(delimiter2);
				append(entry.getValue()).append(delimiter1);
			}
			else
				append(func.apply(entry.getKey(), entry.getValue()))
						.append(delimiter1);
		}
		if (value.size() > 0)
			setLength(buffer.length() - delimiter1.length());

		if (suffix != null) buffer.append(suffix);

		return this;
	}

	/** 添加路径, 判断尾部反斜杠分隔符与path开头分隔符
	 * @param path
	 * @return
	 */
	public final Fmt appendPath(String path) {
		if (path != null && !path.isEmpty()) {
			int len = buffer.length();
			if (len > 0) {
				char c1 = charAt(len - 1);
				char c2 = path.charAt(0);
				boolean b1 = c1 == '/' || c1 == '\\';
				boolean b2 = c2 == '/' || c2 == '\\';
				if (b1 && b2) buffer.setLength(len - 1);
				else if (!b1 && !b2) append('/');
			}
			append(path);
		}
		return this;
	}

	private final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
			99999999, 999999999, Integer.MAX_VALUE };

	/** 追加整数，不足前面补0
	 * @param value 要追加的整数
	 * @param width 宽度，不足前面补0
	 * @return
	 */
	public final Fmt appendInt(int value, int width) {
		int size = 0;
		while (value > sizeTable[size]) ++size;
		int count = width - size - 1;
		while (count-- > 0) buffer.append('0');
		buffer.append(value);
		return this;
	}

	public final Fmt appendNotNull(String text) {
		return text != null && text.length() > 0 ? append(text) : this;
	}

	/** 追加字符串，不足前面补空格
	 * @param text 要追加的文本
	 * @param width 宽度，不足前面补空格
	 * @return
	 */
	public final Fmt appendText(String text, int width) {
		return append(text, width, ' ');
	}

	/** 追加字符串，不足前面补空格
	 * @param text 要追加的文本
	 * @param width 宽度，不足前面补前缀字符
	 * @param prefix 前缀字符
	 * @return
	 */
	public final Fmt appendText(String text, int width, char prefix) {
		if (text == null) return this;
		int count = width - text.length();
		while (count-- > 0) buffer.append(prefix);
		buffer.append(text);
		return this;
	}

	/** 生成指定重复次数的字符串
	 * @param text 需要重复的文本
	 * @param count 重复次数
	 * @return
	 */
	public final Fmt repeat(String text, int count) {
		return repeat(text, count, null);
	}

	/** 生成指定重复次数的字符串
	 * @param text 需要重复的文本
	 * @param count 重复次数
	 * @param delimiter1 分隔符1
	 * @param delimiter2 分隔符2
	 * @return
	 */
	public final Fmt repeat(String text, int count, String delimiter) {
		if (count-- > 0) buffer.append(text);
		if (delimiter != null)
			while (count-- > 0) buffer.append(delimiter).append(text);
		else
			while (count-- > 0) buffer.append(text);
		return this;
	}

	/** 生成指定重复次数的字符串
	 * @param c 需要重复的字符
	 * @param count
	 * @return
	 */
	public final Fmt repeat(char c, int count) {
		while(--count >= 0) buffer.append(c);
		return this;
	}

	/** 将对象以json格式增加
	 * @param value 要格式化的对象
	 * @return
	 */
	public final Fmt appendJson(Object value) {
		if (value == null) {
			appendNull();
			return this;
		}

		Class<?> cls = value.getClass();
		boolean isMatch = true;
		switch (cls.getName()) {
			case "java.lang.String":
				appendJsonString((CharSequence)value);
				break;
			case "java.lang.Integer":
				buffer.append(((Integer)value).intValue());
				break;
			case "java.lang.Long":
				buffer.append(((Long)value).longValue());
				break;
			case "java.lang.Byte":
				buffer.append(((Byte)value).intValue());
				break;
			case "java.lang.Short":
				buffer.append(((Short)value).intValue());
				break;
			case "java.lang.Character":
				charToJson(((Character)value).charValue());;
				break;
			case "java.lang.Boolean":
				buffer.append(((Boolean)value).booleanValue());
				break;
			case "java.lang.Double":
				buffer.append(((Double)value).doubleValue());
				break;
			case "java.lang.Float":
				buffer.append(((Float)value).floatValue());
				break;
			case "java.util.Date":
				append('"').appendDateTime((Date)value).append('"');
				break;
			case "java.util.ArrayList":
				arrayListToJson((ArrayList<?>)value);
				break;
			case "java.util.HashMap":
				mapToJson((Map<?, ?>)value);
				break;
			case "java.util.GregorianCalendar":
				append('"').append((Calendar)value).append('"');
				break;
			case "java.time.LocalDate":
				append('"').append((LocalDate)value).append('"');
				break;
			case "java.time.LocalTime":
				append('"').append((LocalTime)value).append('"');
				break;
			case "java.time.LocalDateTime":
				append('"').append((LocalDateTime)value).append('"');
				break;
			default:
				isMatch = false;
				break;
		}
		if (isMatch) return this;

		//----------------------------
		else if (cls.isArray())
			arrayToJson(value);
		else if (cls.isEnum())
			appendJsonString(((Enum<?>)value).toString());
		else if (value instanceof Map)
			mapToJson((Map<?, ?>)value);
		else if (value instanceof Iterable)
			iterableToJson((Iterable<?>)value);
		else if (value instanceof Number)
			buffer.append(((Number)value).toString());
		else if (value instanceof CharSequence)
			appendJsonString((CharSequence)value);
		else if (value instanceof Calendar)
			append('"').append((Calendar)value).append('"');
		else {
			try {
				Method m = cls.getMethod("toJson");
				append((String) m.invoke(value));
			} catch (Exception e) {
				objectToJson(value);
			}
		}

		return this;
	}

	private void objectToJson(Object value) {
		append('{');
		Class<?> cls = value.getClass();
		Method[] ms = cls.getMethods();
		boolean first = false;

		// 先处理公共属性的字段
		Field[] fs = cls.getFields();
		for (int i = 0, n = fs.length; i < n; ++i) {
			try {
				for (; i < n; ++i) {
					Field f = fs[i];
					String fn = f.getName();
					f.setAccessible(true);
					Object obj = f.get(value);
					if (obj == null) continue;
					if (!first) first = true;
					else append(',').append(' ');
					append('"');
					append(fn).append("\": ");
					appendJson(obj);
				}
			} catch (Exception e) { }
		}

		for (int i = 0, n = ms.length; i < n; ++i) {
			try {
				for (; i < n; ++i) {
					Method m = ms[i];
					String mn = m.getName();
					if (mn.length() < 4
							|| !mn.startsWith("get")
							|| mn.equals("getClass")
							|| m.getTypeParameters().length > 0)
						continue;
					m.setAccessible(true);
					Object obj = m.invoke(value);
					if (obj == null) continue; //属性为空则忽略该属性
					if (!first) first = true;
					else append(',').append(' ');
					append('"');
					char c = mn.charAt(3);
					append((c >= 'A' && c <= 'Z') ? (char)(c + 0x20) : c)
							.append(mn, 4, mn.length())
							.append('"').append(':').append(' ');
					appendJson(obj);
				}
			} catch (Exception e) { }
		}

		append('}');
	}

	private final void iterableToJson(Iterable<?> value) {
		Iterator<?> iter = value.iterator();
		append('[');
		if (iter.hasNext()) appendJson(iter.next());
		while (iter.hasNext()) {
			append(',').append(' ');
			appendJson(iter.next());
		}
		append(']');
	}

	private final void charToJson(char value) {
		append('"');
		switch (value) {
			case '\b': append('\\').append('b'); break;
			case '\f': append('\\').append('f'); break;
			case '\n': append('\\').append('n'); break;
			case '\r': append('\\').append('r'); break;
			case '\t': append('\\').append('t'); break;
			case '"': case '\'': case '\\': case '/':
				append('\\').append(value);
				break;
			default: append(value);
		}
		append('"');
	}

	private void mapToJson(Map<?, ?> value) {
		append('{');
		Iterator<?> iter = value.entrySet().iterator();
		if (iter.hasNext()) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
			if (entry != null) {
				append('"').append(entry.getKey().toString())
						.append('"').append(':').append(' ');
				appendJson(entry.getValue());
			}
		}
		while (iter.hasNext()) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
			if (entry == null) continue;
			append(',').append(' ').append('"')
					.append(entry.getKey().toString()).append('"')
					.append(':').append(' ');
			appendJson(entry.getValue());
		}
		append('}');
	}

	private void arrayToJson(Object value) {
		append('[');

		String pri_cls_name = value.getClass().getComponentType().getName();
		int index = getPrimitiveTypeIndex(pri_cls_name);

		boolean first = true;
		for (int i = 0, n = Array.getLength(value); i < n; ++i) {
			if (first) first = false;
			else buffer.append(',').append(' ');

			switch (index) {
				case 0:
					buffer.append(Array.getBoolean(value, i));
					break;
				case 1:
					buffer.append(Array.getByte(value, i));
					break;
				case 2:
					charToJson(Array.getChar(value, i));
					break;
				case 3:
					buffer.append(Array.getDouble(value, i));
					break;
				case 4:
					buffer.append(Array.getFloat(value, i));
					break;
				case 5:
					buffer.append(Array.getInt(value, i));
					break;
				case 6:
					buffer.append(Array.getLong(value, i));
					break;
				case 7:
					buffer.append(Array.getShort(value, i));
					break;
				default:
					appendJson(Array.get(value, i));
					break;
			}
		}

		append(']');
	}

	private final void arrayListToJson(ArrayList<?> value) {
		append('[');
		int n = value.size();
		if (n > 0) appendJson(value.get(0));
		for (int i = 1; i < n; ++i) {
			append(',').append(' ');
			appendJson(value.get(i));
		}
		append(']');
	}

	/*
	private static char toLowerCase(char c) {
		return (c >= 'A' && c <= 'Z') ? (char)(c + 0x20) : c;
	}

	private static char toUpperCase(char c) {
		return (c >= 'a' && c <= 'z') ? (char)(c - 0x20) : c;
	}
	*/

	/** 转换成16进制 */
	public final Fmt appendHex(byte[] bytes) {
		buffer.appendHex(bytes, null);
		return this;
	}

	/** 转换成16进制 */
	public final Fmt appendHex(byte[] bytes, char delimiter) {
		buffer.appendHex(bytes, new char[] {delimiter});
		return this;
	}

	/** 转换成16进制 */
	public final Fmt appendHex(int value) {
		buffer.appendHex(value);
		return this;
	}

	/** 转换成16进制 */
	public final Fmt appendHex(final long value) {
		buffer.appendHex(value);
		return this;
	}

	/** 转换成base64 */
	public final Fmt appendBase64(String text) {
		buffer.appendBase64(text, false);
		return this;
	}

	/** 转换成base64 */
	public final Fmt appendBase64(String text, boolean lineBreak) {
		buffer.appendBase64(text, lineBreak);
		return this;
	}

	/** base64编码
	 * @param bytes 要编码的字节数组
	 * @param lineBreak 是否每76个字符换行标志
	 * @return 编码后的字符串
	 */
	public final Fmt appendBase64(final byte[] bytes, boolean lineBreak) {
		buffer.appendBase64(bytes, lineBreak);
		return this;
	}

	/** 以json字符串方式追加字符串, 自动对字符串进行json方式转义 */
	public final Fmt appendJsonString(CharSequence value) {
		if(value == null) appendNull();
		else {
			buffer.append('"');
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
				if (finded == -1) buffer.append(c);
				else buffer.append('\\').append((char) finded);
			}
			buffer.append('"');
		}
		return this;
	}

}



