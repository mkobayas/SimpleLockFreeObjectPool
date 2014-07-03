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

package org.mk300.pool.sample.keyed.test;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.mk300.pool.BorrowSt;
import org.mk300.pool.KeyedSimpleLockFreeObjectPool;
import org.mk300.pool.sample.keyed.KeyedSampleCounterControllerFactory;
import org.mk300.pool.sample.simple.SampleCounter;

/**
 * 
 * @author mkobayas@redhat.com
 *
 */
public class KeyedSimpleLockFreeObjectPoolPefromanceTest {

	@Option(name="-h", usage="help")
	public static boolean help;

	@Option(name="-t", metaVar="th_num", usage="threads count")
	public static int th_num = 500;

	
	@Option(name="-p", metaVar="poolSize", usage="pool Size")
	public static int poolSize = 20;
	

	@Option(name="-d", metaVar="duration", usage="duration")
	public static long time = 3000;

	@Option(name="-s", metaVar="BorrowSt", usage="FIRST | RANDOM | RANDOM_FIRST | THREAD")
	public static BorrowSt st = BorrowSt.RANDOM_FIRST;
	

	@Option(name="-w", metaVar="wait", usage="wait")
	public static long wait = 0;
	
	public static KeyedSimpleLockFreeObjectPool<Integer, SampleCounter> pool;
	public static KeyedSampleCounterControllerFactory controller;
	
	public static void main(String[] args) throws Exception {
		
		// parse argument
		KeyedSimpleLockFreeObjectPoolPefromanceTest app = new KeyedSimpleLockFreeObjectPoolPefromanceTest();
        CmdLineParser parser = new CmdLineParser(app);
        try {
            parser.parseArgument(args);    
        } catch (CmdLineException e) {
            System.err.println(e);
            parser.printUsage(System.err);
            System.exit(1);
        }
		
        if(help) {
            parser.printUsage(System.err);
            System.exit(1);
        }
		
		testThread();
		
    }
	
	private static void testThread() throws Exception {
		
		controller = new KeyedSampleCounterControllerFactory();
		pool = new KeyedSimpleLockFreeObjectPool<Integer, SampleCounter>(poolSize, controller, st);	
		
		System.out.println("START");

		
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch endLatch = new CountDownLatch(th_num);
		final AtomicLong count = new AtomicLong(0);
		for(int i=0; i<th_num ; i++) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {

					long localCount = 0;
					try {
						startLatch.await();
						long start = System.currentTimeMillis();
						long end = start + time;
						while(System.currentTimeMillis() < end) {

							int key = (int)localCount % 10;
							SampleCounter counter = null;
							try {
								counter = pool.borrow(key);
								
								counter.increment();
								
								if(wait > 0) {
									Thread.sleep(wait);
								}
								
							} finally {
								if(counter != null) {
									pool.release(key, counter);
								}
							}
							
							localCount++;
						}
						
						end = System.currentTimeMillis();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						count.addAndGet(localCount);
						endLatch.countDown();
					}
				}
				
			});
			t.start();
		}

		long start = System.currentTimeMillis();
		startLatch.countDown();
		endLatch.await();
		long end = System.currentTimeMillis();
		
		long ela = end - start;
		
		pool.destroy();
		
		
		System.out.println(String.format("Exec time = %s ms" , String.format("%,d", ela )));
		System.out.println(String.format("%4s Threads : throghput = %12s", th_num, String.format("%,d",  (long)(((double)count.get())*1000d/ela) )));
		System.out.println(String.format("Exec sum    = %12s", String.format("%,d",  count.get() )));
	}
}
