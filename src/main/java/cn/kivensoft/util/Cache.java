package cn.kivensoft.util;

import cn.kivensoft.util.impl.LruCache;
import cn.kivensoft.util.impl.WeakCache;

/** 缓存接口, 系统默认实现LRUCache和WeakCache两种
 * @author kiven lee
 * @version 1.0
 * @date 2020-01-05
 */
public interface Cache<K, V> {
	enum CacheType { LRU, WeakRefence };

	static <K, V> Cache<K, V> create(CacheType cacheType) {
		return create(cacheType, false, 0, 0, 0);
	}

	static <K, V> Cache<K, V> create(CacheType cacheType, boolean isSynchronlzed) {
		return create(cacheType, isSynchronlzed, 0, 0, 0);
	}

	static <K, V> Cache<K, V> create(CacheType cacheType, boolean isSynchronlzed,
			int cacheSize, long expire, int initSize) {
		if (cacheType == CacheType.WeakRefence)
			return new WeakCache<>(isSynchronlzed);	
		else
			return new LruCache<>(cacheSize, expire, isSynchronlzed, initSize);
	}

	static <K, V> Cache<K, V> createLruCache(int cacheSize, long expire,
			boolean isSynchronlzed, int initSize) {
		return new LruCache<>(cacheSize, expire, isSynchronlzed, initSize);
	}

	/** 获取缓存值
	 * @param key 键
	 * @return 值
	 */
	V get(K key);
	
	/** 设置缓存值
	 * @param key 键
	 * @param value 值
	 * @return 原来的值, 不存在则返回null
	 */
	V put(K key, V value);

	/** 移除缓存值
	 * @param key 键
	 * @return 被删除的值, 不存在返回null
	 */
	V remove(K key);
	
	/** 清除所有缓存值 */
	void clear();
	
	/** 清理回收无效值 */
	void cycle();
	
	/** 获取缓存大小 */
	int size();
	
	/** 判断缓存是否为空 */
	boolean isEmpty();
	
	/** 获取可缓存总数 */
	int getCacheSize();	

	/** 判断是否包含该键的缓存
	 * @param key 键
	 * @return true: 包含, false: 不包含
	 */
	boolean containsKey(K key);

	/** 判断是否线程安全
	 * @return true: 线程安全, false: 线程不安全
	 */
	boolean isSync();
}
