package io.mk.pool;

public interface KeyedObjectControllerFactory<K, T> {
	
	public ObjectController<T> createObjectController(K key);

}
