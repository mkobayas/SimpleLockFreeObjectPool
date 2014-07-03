package org.mk300.pool;

public class LinerObjectPool<T> implements ObjectPool<T> {
	
	private final int index;
	private final int totalSize;
	
	private volatile T value;
	private final LinerObjectPool<T> next;
	
	private final boolean isRoot;
	
	/**
	 * このプールが保持しているvalueはvolatileなため、検索時にメモリバリアによるメモリ同期が発生する。
	 * 検索専用にvolatileではない配列にvalueを保持させ、indexの高速検索を可能とする。
	 * rentedはスレッド間のメモリ可視性により、正しいオブジェクト参照が見えない可能性が在るため、
	 * 実際の検索でヒットしなかった場合にはvolatileなvalueの検索も実施される。
	 */
	private final Object[] rented;
	
	public LinerObjectPool(int totalSize) {
		this(totalSize, 0, true);
	}
	

	private LinerObjectPool(int totalSize, int index, boolean isRoot) {
		this.isRoot = isRoot;
		this.totalSize = totalSize;
		this.index = index;
		
		if(index < totalSize) {
			next = new LinerObjectPool<T>(totalSize, index + 1, false);
		} else {
			next = null;
		}
		
		value = null;
		
		if(this.isRoot) {
			rented = new Object[totalSize];
		} else {
			rented = null;
		}
	}
	
	public T getValue(int index) {
		if(this.totalSize <= index) {
			throw new RuntimeException("bad operation totalSize=" + totalSize + ", index=" + index);
		}
		
		if(this.index == index) {
			return value;
		} else {
			return next.getValue(index);
		}
	}
	
	public void setValue(int index, T value) {
		if(this.totalSize <= index) {
			throw new RuntimeException("bad operation totalSize=" + totalSize + ", index=" + index);
		}
		
		if(this.index == index) {
			this.value = value;
		} else {
			next.setValue(index, value);
		}
		
		if(isRoot) {
			rented[index] = value;
		}
		
	}
	
	public int search(T value) {
		
		// non volatileな検索
		for(int i=0; i<totalSize; i++) {
			if(rented[i] == value) {
				return i;
			}
		}
		
		// JMMの可視性により、non volatileな検索で検索出来ない場合がある。
		//  volatileな検索を実施して、確実にチェックする
		return searchPool(value);
	}
	
	private int searchPool(T value) {
		if(this.value == value) {
			return index;
		} else {
			if( next != null) {
				return next.search(value);
			} else {
				return -1;
			}
		}
	}

}
