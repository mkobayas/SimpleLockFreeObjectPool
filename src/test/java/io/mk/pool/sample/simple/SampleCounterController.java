package io.mk.pool.sample.simple;

import io.mk.pool.ObjectController;

import java.util.concurrent.atomic.AtomicLong;


public class SampleCounterController implements ObjectController<SampleCounter> {

	static AtomicLong total = new AtomicLong(0);
	
	@Override
	public SampleCounter create() {
		return new SampleCounter();
	}

	@Override
	public void destory(SampleCounter obj) {
		total.addAndGet(obj.get());
	}

	public long getTotal() {
		return total.get();
	}
}
