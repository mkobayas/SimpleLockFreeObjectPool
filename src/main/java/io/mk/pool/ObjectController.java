package io.mk.pool;

public interface ObjectController<T> {

	public T create();
	
	public void destory(T obj);
	
}
