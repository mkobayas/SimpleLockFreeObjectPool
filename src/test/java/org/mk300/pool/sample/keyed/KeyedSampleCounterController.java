package org.mk300.pool.sample.keyed;

import org.mk300.pool.ObjectController;
import org.mk300.pool.sample.simple.SampleCounter;


public class KeyedSampleCounterController implements ObjectController<SampleCounter> {
	
	private final int key;
	
	public KeyedSampleCounterController(int key) {
		this.key = key;
	}
	
	@Override
	public SampleCounter create() {
		return new SampleCounter();
	}

	@Override
	public void destory(SampleCounter obj) {
		System.out.println(String.format("Key= %3s, Count : %12s", key, String.format("%,d", obj.get() )));
	}

}
