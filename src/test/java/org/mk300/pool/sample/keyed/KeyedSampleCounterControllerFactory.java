/*
 * Copyright 2014 Masazumi Kobayashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mk300.pool.sample.keyed;

import org.mk300.pool.KeyedObjectControllerFactory;
import org.mk300.pool.ObjectController;
import org.mk300.pool.sample.simple.SampleCounter;

/**
 * 
 * @author mkobayas@redhat.com
 *
 */
public class KeyedSampleCounterControllerFactory implements KeyedObjectControllerFactory<Integer, SampleCounter> {

	
	@Override
	public ObjectController<SampleCounter> createObjectController(Integer key) {
		
		KeyedSampleCounterController controller = new KeyedSampleCounterController(key); 
		return controller;
	}

}
