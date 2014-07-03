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

import org.mk300.pool.ObjectController;
import org.mk300.pool.sample.simple.SampleCounter;

/**
 * 
 * @author mkobayas@redhat.com
 *
 */
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
