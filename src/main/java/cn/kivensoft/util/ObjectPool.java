package cn.kivensoft.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.kivensoft.util.impl.ObjectPoolImpl;

/** 对象池接口, 实现对可复用对象的对象池管理, 线程安全, 采取非锁方式实现
 * @author kiven lee
 * @version 1.0
 * @date 2018-10-11
 */
public interface ObjectPool<T> {

	static <T> ObjectPool<T> create(Supplier<T> objFactory) {
		return new ObjectPoolImpl<>(objFactory);
	}

	static <T> ObjectPool<T> create(Supplier<T> objFactory, Consumer<T> recycleFunc) {
		return new ObjectPoolImpl<>(objFactory, recycleFunc);
	}

	interface Item<T> {
		T get();
		void recycle();
	}

	void get(Consumer<T> consumer);
	Item<T> get();
	void clear();
}
