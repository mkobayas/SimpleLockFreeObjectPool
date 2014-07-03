package org.mk300.pool.sample.simple;

public class SampleCounter {

	private volatile int count = 0;
	
	public void increment() {
		count++;
	}
	
	public int get() {
		return count;
	}
}
