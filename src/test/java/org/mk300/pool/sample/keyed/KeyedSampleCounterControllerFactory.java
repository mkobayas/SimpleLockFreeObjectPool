package org.mk300.pool.sample.keyed;

import org.mk300.pool.KeyedObjectControllerFactory;
import org.mk300.pool.ObjectController;
import org.mk300.pool.sample.simple.SampleCounter;


public class KeyedSampleCounterControllerFactory implements KeyedObjectControllerFactory<Integer, SampleCounter> {

	
	@Override
	public ObjectController<SampleCounter> createObjectController(Integer key) {
		
		KeyedSampleCounterController controller = new KeyedSampleCounterController(key); 
		return controller;
	}

}
