package cn.kivensoft.util;

/** 可修改的IntNumber类, 适用于lambda表达式中修改不可变对象
 * @author kiven lee
 * @version 1.0
 * @date 2019-03-05
 */
final public class IntNumber extends Number {
	private static final long serialVersionUID = 1L;
	
	private int value;

	public IntNumber() {
		super();
	}

	public IntNumber(int value) {
		super();
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	/** 加法 */
	public void plus(int value) {
		this.value += value;
	}
	
	/** 减法 */
	public void minus(int value) {
		this.value -= value;
	}
	
	/** 乘法 */
	public void multiply(int value) {
		this.value *= value;
	}
	
	/** 除法 */
	public void divide(int value) {
		this.value /= value;
	}

	/** 求余 */
	public void mod(int value) {
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
		return Integer.toString(value);
	}
	
}
