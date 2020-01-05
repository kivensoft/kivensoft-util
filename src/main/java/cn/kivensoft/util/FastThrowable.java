package cn.kivensoft.util;

/** 快速异常类, 不创建异常堆栈
 * @author kiven lee
 * @version 1.0
 */
public class FastThrowable extends Throwable {
	private static final long serialVersionUID = 1L;

	public FastThrowable(String message) {
		super(message, null, false, false);
	}

}
