package io.mk.pool.sample.keyed;

import io.mk.pool.KeyedObjectControllerFactory;
import io.mk.pool.ObjectController;
import io.mk.pool.sample.simple.SampleCounter;

public class KeyedSampleCounterControllerFactory implements KeyedObjectControllerFactory<Integer, SampleCounter> {

	
	@Override
	public ObjectController<SampleCounter> createObjectController(Integer key) {
		
		KeyedSampleCounterController controller = new KeyedSampleCounterController(key); 
		return controller;
	}

}
