package io.mk.pool;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class SimpleLockFreeObjectPool<T> {

	static final int ST_NULL= 0;
	static final int ST_FOR_RENT= 1;
	static final int ST_RENTED_OUT= 2;
	static final int ST_DESTROY= 3;
	
	private final AtomicIntegerArray poolSlot;
	private final ObjectPool<T> pool;
	private final int size;
	private final AtomicBoolean isActive;
	
	private final Semaphore sem;
	
	private final ObjectController<T> controller;

	private final BorrowSt st;
	
	// for statistics
	private final AtomicInteger objectCount = new AtomicInteger(0);
	private final AtomicInteger createCount = new AtomicInteger(0);
	private final AtomicInteger destroyCount = new AtomicInteger(0);
	private final AtomicInteger creationErrorCount = new AtomicInteger(0);
	private final AtomicInteger destroyErrorCount = new AtomicInteger(0);
	
	public SimpleLockFreeObjectPool(int size, ObjectController<T> controller) {
		this(size, controller, BorrowSt.RANDOM_FIRST);
	}
	

	public SimpleLockFreeObjectPool(int size, ObjectController<T> controller, BorrowSt st) {
		this.st = st;
		
		this.size = size;
		poolSlot = new AtomicIntegerArray(size);
		
		if(st.equals(BorrowSt.FIRST)) {
			pool = new LinerObjectPool<T>(size);
		} else {
			pool = new ArrayObjectPool<T>(size);
		}
		
		for(int i=0; i<size; i++) {
			poolSlot.set(i, ST_NULL);
		}
		
		sem = new Semaphore(size, false);
		
		this.controller = controller;
		isActive = new AtomicBoolean(true);
	}
	
	public T borrow() {
		if(isActive.get() == false) {
			throw new RuntimeException("This pool was already destroyed");
		}
		
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		try {
			T rent = search();
			return rent;
			
		} catch (Throwable e) {
			sem.release();
			throw new RuntimeException(e);
		}
	}
	
	public void release(T obj) {
		if( obj == null) {
			throw new RuntimeException("Dont release null object");
		}
		
		
		int index = pool.search(obj);
		
		if(index == -1) {
			// already removed.
		} else {
			if(poolSlot.compareAndSet(index, ST_RENTED_OUT ,ST_FOR_RENT)) {
				sem.release();
			} else {
				// release is called double.
			}
		}
	}
	
	public void remove(T obj) {
		if( obj == null) {
			throw new RuntimeException("Dont remove null object");
		}
		
		int index = pool.search(obj);
		
		if(index == -1) {
			// already removed.
		} else {
			pool.setValue(index, null);
			
			if(poolSlot.compareAndSet(index, ST_RENTED_OUT ,ST_NULL)) {
				sem.release();
			}
			
		}
		
		// Even if no effect, given obj shuoud be destroyed. 
		try {
			controller.destory(obj);
			destroyCount.incrementAndGet();
		} catch(Exception e) {
			destroyErrorCount.incrementAndGet();
		}
	}
	
	private T search() {
		
		int i;
		
		if(st.equals(BorrowSt.FIRST)) {
			i = -1;
		} else if(st.equals(BorrowSt.RANDOM)) {
			i = (ThreadLocalRandom.current().nextInt() & 0x7FFFFFFF) % size -1;
		}  else if(st.equals(BorrowSt.RANDOM_FIRST)) {
			int div = size - sem.availablePermits();
			i = (ThreadLocalRandom.current().nextInt() & 0x7FFFFFFF) % div -1;
		} else {
			i = (int) (Thread.currentThread().getId() % size) -1;
		}
		
		while(true) {

			i++;
			if(i >= size) {
				i = 0;
			}
			
			switch(poolSlot.get(i)) {
			case ST_RENTED_OUT:
				continue;
			case ST_FOR_RENT:
				if(poolSlot.compareAndSet(i, ST_FOR_RENT, ST_RENTED_OUT)) {
					return pool.getValue(i); 
				}
				break;
			case ST_NULL:
				if(poolSlot.compareAndSet(i, ST_NULL, ST_RENTED_OUT)) {
					try {
						T obj = create();
						pool.setValue(i, obj);
						return obj;
					} catch (RuntimeException e) {
						poolSlot.set(i, ST_NULL);
						throw e;
					}
				}
			}
			
			
		}
	}
	
	private T create() {
		
		T obj;
		try {
			obj = controller.create();
		} catch (RuntimeException e) {
			creationErrorCount.incrementAndGet();
			throw e;
		}
		
		if(obj == null) {
			creationErrorCount.incrementAndGet();
			throw new RuntimeException("Created object must not be null!");
		}
		
		objectCount.incrementAndGet();
		createCount.incrementAndGet();
		return obj;
	}
	
    /**
     * Clears any objects sitting idle in the pool by removing them from the
     * idle instance pool and then invoking the configured 
     * {@link PoolableObjectFactory#destroyObject(Object)} method on each idle
     * instance. 
     * 
     * <p> Implementation notes:
     * <ul><li>This method does not destroy or effect in any way instances that are
     * checked out of the pool when it is invoked.</li>
     * <li>Invoking this method does not prevent objects being
     * returned to the idle instance pool, even during its execution. It locks
     * the pool only during instance removal. Additional instances may be returned
     * while removed items are being destroyed.</li>
     * <li>Exceptions encountered destroying idle instances are swallowed.</li></ul></p>
     */
	public void clear() {
		
		int drainCount = sem.drainPermits();

		int clearMarkCount = 0;
		for(int i=0; i<size ;i++) {
			if(poolSlot.compareAndSet(i, ST_FOR_RENT, ST_RENTED_OUT)) {
				try {
					controller.destory(pool.getValue(i));
					destroyCount.incrementAndGet();	
				} catch(Exception e) {
					destroyErrorCount.incrementAndGet();
				}
				
				pool.setValue(i, null);
				poolSlot.set(i, ST_NULL);
					
				objectCount.decrementAndGet();
				clearMarkCount++;
			}
			
			if(drainCount == clearMarkCount) {
				break;
			}
		}
		
		sem.release(drainCount);
		
	}

	public void destroy() {
		if( isActive.compareAndSet(true, false)) {
			int drainCount = sem.drainPermits();
			
			int destroyMarkCount = 0;
			for(int i=0; i<size ;i++) {
				if(poolSlot.compareAndSet(i, ST_FOR_RENT, ST_RENTED_OUT)) {
					try {
						controller.destory(pool.getValue(i));
						destroyCount.incrementAndGet();
					} catch(Exception e) {
						destroyErrorCount.incrementAndGet();
					}
					
					pool.setValue(i, null);
					poolSlot.set(i, ST_DESTROY);
						
					objectCount.decrementAndGet();
					
					destroyMarkCount++;
				} else if(poolSlot.compareAndSet(i, ST_NULL, ST_DESTROY)) {
					destroyMarkCount++;
				}
				
				if(drainCount == destroyMarkCount) {
					break;
				}
			}
			
			sem.release(drainCount);
		}
	}
	
	public int getObjectCount() {
		return objectCount.get();
	}

	public int getCreateCount() {
		return createCount.get();
	}
	
	public int getDestroyCount() {
		return destroyCount.get();
	}
	
	public int getCreationErrorCount() {
		return creationErrorCount.get();
	}
}
