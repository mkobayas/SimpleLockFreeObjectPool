package org.mk300.pool;

public interface KeyedObjectControllerFactory<K, T> {
	
	public ObjectController<T> createObjectController(K key);

}
