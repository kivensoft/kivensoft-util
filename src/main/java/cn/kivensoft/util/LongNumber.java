package cn.kivensoft.util;

/** 可修改的IntNumber类, 适用于lambda表达式中修改不可变对象
 * @author kiven lee
 * @version 1.0
 * @date 2019-03-05
 */
final public class LongNumber extends Number {
	private static final long serialVersionUID = 1L;
	
	private long value;

	public LongNumber() {
		super();
	}

	public LongNumber(long value) {
		super();
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}
	
	/** 加法 */
	public void plus(long value) {
		this.value += value;
	}
	
	/** 减法 */
	public void minus(long value) {
		this.value -= value;
	}
	
	/** 乘法 */
	public void multiply(long value) {
		this.value *= value;
	}
	
	/** 除法 */
	public void divide(long value) {
		this.value /= value;
	}

	/** 求余 */
	public void mod(long value) {
		this.value %= value;
	}
	
	/** 自增 */
	public void increment() {
		++value;
	}
	
	/** 自减 */
	public void decrement() {
		--value;
	}
	
	@Override
	public int intValue() {
		return (int)value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return (float)value;
	}

	@Override
	public double doubleValue() {
		return (double)value;
	}
	
	@Override
	public String toString() {
		return Long.toString(value);
	}

}
