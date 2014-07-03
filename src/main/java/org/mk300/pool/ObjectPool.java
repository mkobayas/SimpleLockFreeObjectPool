package org.mk300.pool;


public interface ObjectPool<T> {
	
	public T getValue(int index);
	
	public void setValue(int index, T value);
	
	public int search(T value);
}
