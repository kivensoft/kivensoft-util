package cn.kivensoft.util;

/** 对象引用类, 适用于lambda函数中的可修改对象
 * @author kiven lee
 * @version 1.0
 * @date 2019-06-05
 */
final public class ObjRef<T> {
	private T ref;

	public ObjRef() {
		super();
	}

	public ObjRef(T ref) {
		super();
		this.ref = ref;
	}

	public final T get() {
		return ref;
	}

	public final void set(T ref) {
		this.ref = ref;
	}
}
