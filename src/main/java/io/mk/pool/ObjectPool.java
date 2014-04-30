package io.mk.pool;


public interface ObjectPool<T> {
	
	public T getValue(int index);
	
	public void setValue(int index, T value);
	
	public int search(T value);
}
