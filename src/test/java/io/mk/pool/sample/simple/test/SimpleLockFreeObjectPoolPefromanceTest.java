package io.mk.pool.sample.simple.test;

import io.mk.pool.BorrowSt;
import io.mk.pool.SimpleLockFreeObjectPool;
import io.mk.pool.sample.simple.SampleCounter;
import io.mk.pool.sample.simple.SampleCounterController;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;


public class SimpleLockFreeObjectPoolPefromanceTest {

	@Option(name="-h", usage="help")
	public static boolean help;

	@Option(name="-t", metaVar="th_num", usage="threads count")
	public static int th_num = 500;

	
	@Option(name="-p", metaVar="poolSize", usage="pool Size")
	public static int poolSize = 100;
	

	@Option(name="-d", metaVar="duration", usage="duration")
	public static long time = 3000;
	
	@Option(name="-s", metaVar="BorrowSt", usage="BorrowSt")
	public static BorrowSt st = BorrowSt.RANDOM;
	

	@Option(name="-w", metaVar="wait", usage="wait")
	public static long wait = 0;
	
	public static SimpleLockFreeObjectPool<SampleCounter> pool;
	public static SampleCounterController controller;
	
	public static void main(String[] args) throws Exception {
		
		// parse argument
		SimpleLockFreeObjectPoolPefromanceTest app = new SimpleLockFreeObjectPoolPefromanceTest();
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
		
		controller = new SampleCounterController();
		pool = new SimpleLockFreeObjectPool<SampleCounter>(poolSize, controller, st);		
		
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

							SampleCounter counter = null;
							try {
								counter = pool.borrow();
								
								counter.increment();
								
								if(wait > 0) {
									Thread.sleep(wait);
								}
								
							} finally {
								if(counter != null) {
									pool.release(counter);
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
		System.out.println(String.format("Counter sum = %12s", String.format("%,d", controller.getTotal())));
		
		System.out.println("pool.getCreateCount          = " + pool.getCreateCount());
		System.out.println("pool.getObjectCount()        = " + pool.getObjectCount());
		System.out.println("pool.getDestroyCount()       = " + pool.getDestroyCount());
		System.out.println("pool.getCreationErrorCount() = " + pool.getCreationErrorCount());
	}
}
