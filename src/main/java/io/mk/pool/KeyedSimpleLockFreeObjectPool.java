package io.mk.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyedSimpleLockFreeObjectPool<K, T> {

	private final ConcurrentHashMap<K, SimpleLockFreeObjectPool<T>> map;
	private final KeyedObjectControllerFactory<K, T> controller;
	private final int size;
	private final AtomicBoolean isActive;
	private final BorrowSt st;
	
	public KeyedSimpleLockFreeObjectPool(int size, KeyedObjectControllerFactory<K, T> controller) {
		this(size, controller, BorrowSt.RANDOM_FIRST);
	}
	

	public KeyedSimpleLockFreeObjectPool(int size, KeyedObjectControllerFactory<K, T> controller, BorrowSt st) {
		this.size = size;
		this.map = new ConcurrentHashMap<K, SimpleLockFreeObjectPool<T>>();
		this.controller = controller;
		isActive = new AtomicBoolean(true);
		this.st = st;
	}
	
	
	public T borrow(K key) {
		if(isActive.get() == false) {
			throw new RuntimeException("This pool was already destroyed");
		}
		
		SimpleLockFreeObjectPool<T> pool = map.get(key);
		if(pool == null) {
			final SimpleLockFreeObjectPool<T> newPool = new SimpleLockFreeObjectPool<T>(size, controller.createObjectController(key), st);
			
			pool = map.putIfAbsent(key, newPool);
			
			if(pool == null) {
				pool = newPool;
			}
		}
		
		return pool.borrow();
	}
	
	public void release(K key, T obj) {
		map.get(key).release(obj);
	}
	
	public void remove(K key, T obj) {
		map.get(key).remove(obj);
	}

	public void clear() {
		for(SimpleLockFreeObjectPool<T> pool : map.values()) {
			pool.clear();
		}
	}
	
	public void clear(K key) {
		SimpleLockFreeObjectPool<T> pool = map.remove(key);
		
		if(pool != null) {
			pool.clear();
		}
	}
	
	public void destroy() {
		if( isActive.compareAndSet(true, false)) {
			for(SimpleLockFreeObjectPool<T> pool : map.values()) {
				pool.destroy();
			}
		}
	}

	public int getMaxActive() {
		return size;
	}
}
