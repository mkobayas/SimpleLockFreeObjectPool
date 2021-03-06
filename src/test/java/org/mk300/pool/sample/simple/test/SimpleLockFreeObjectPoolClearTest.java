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

package org.mk300.pool.sample.simple.test;


import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.mk300.pool.BorrowSt;
import org.mk300.pool.SimpleLockFreeObjectPool;
import org.mk300.pool.sample.simple.SampleCounter;
import org.mk300.pool.sample.simple.SampleCounterController;

/**
 * 
 * @author mkobayas@redhat.com
 *
 */
public class SimpleLockFreeObjectPoolClearTest {

	@Option(name="-h", usage="help")
	public static boolean help;
	
	@Option(name="-n", metaVar="count", usage="count per thread")
	public static int count = 10000000;

	@Option(name="-t", metaVar="th_num", usage="threads count")
	public static int th_num = 8;

	@Option(name="-l", metaVar="loop", usage="Loop count")
	public static int loop = 1;
	
	@Option(name="-p", metaVar="poolSize", usage="pool Size")
	public static int poolSize = 20;

	@Option(name="-s", metaVar="BorrowSt", usage="FIRST | RANDOM | RANDOM_FIRST | THREAD")
	public static BorrowSt st = BorrowSt.RANDOM_FIRST;
	
	public static AtomicLong total = new AtomicLong(0);
	
	
	public static SimpleLockFreeObjectPool<SampleCounter> pool;
	public static SampleCounterController controller;
	
	public static void main(String[] args) throws Exception {
		
		// parse argument
		SimpleLockFreeObjectPoolClearTest app = new SimpleLockFreeObjectPoolClearTest();
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
        
        
		controller = new SampleCounterController();
		pool = new SimpleLockFreeObjectPool<SampleCounter>(poolSize, controller, st);
		
		Thread[] t = new Thread[th_num];
		for(int i=0; i<t.length ; i++) {
			t[i] = new Thread(new LoadThread(i+1, count, loop));
		}

		System.out.println("START");
		long startT = System.currentTimeMillis();
		for(int i=0; i<t.length ; i++) {
			t[i].start();
		}
		
		for(int i=0; i< 10 ; i++) {
			Thread.sleep(1000);	
			System.out.println("pool.getCreateCount = " + pool.getCreateCount());
			System.out.println("pool.getObjectCount() = " + pool.getObjectCount());
			System.out.println("pool.getDestroyCount() = " + pool.getDestroyCount());
			System.out.println("pool.getCreationErrorCount() = " + pool.getCreationErrorCount());
			System.out.println("- clear() -----------------------------------------------");
			pool.clear();
		}
		
		for(int i=0; i<t.length ; i++) {
			t[i].join();
		}
		long end = System.currentTimeMillis();
		System.out.println((end -startT) + " ms " + (int)(count*th_num*loop/((end-startT)/1000d)) + "tx/sec");
		System.out.println("total     = " + total);
		
		pool.destroy();
		
		long sum = controller.getTotal();
		System.out.println("Counter sum = " + sum);
		
		System.out.println("pool.getCreateCount = " + pool.getCreateCount());
		System.out.println("pool.getObjectCount() = " + pool.getObjectCount());
		System.out.println("pool.getDestroyCount() = " + pool.getDestroyCount());
		System.out.println("pool.getCreationErrorCount() = " + pool.getCreationErrorCount());
		
		
    }
	
	static class LoadThread implements Runnable {

		private int threadNum;
		private int count;
		private int loop;
		
		public int subTotal;
		
		public LoadThread(int threadNum, int count, int loop) {
			this.threadNum = threadNum;
			this.count = count;
			this.loop = loop;
		}
		
		@Override
		public void run() {
			Thread.currentThread().setName("TH-" + this.threadNum);
			
			for(int j=1 ; j<=loop; j++) {
				
				long startT = System.currentTimeMillis();
				
				for(int i=0 ; i< count ; i++) {
					
					SampleCounter counter = null;
					try {
						counter = pool.borrow();
						
						counter.increment();
						
					} finally {
						if(counter != null) {
							pool.release(counter);
						}
					}
					
					subTotal++;
				}
				long end = System.currentTimeMillis();
				
				System.out.println(threadNum + ": subTotal=" + subTotal + " : " + (end - startT)  + "ms " + (int)(count/((end-startT)/1000d)) + "tx/sec ");
				
			}
		
			total.addAndGet(subTotal);
		}
		
	}
}
