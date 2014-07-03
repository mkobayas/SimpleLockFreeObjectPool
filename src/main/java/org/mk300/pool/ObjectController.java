package org.mk300.pool;

public interface ObjectController<T> {

	public T create();
	
	public void destory(T obj);
	
}
