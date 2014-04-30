package io.mk.pool;

public class ArrayObjectPool<T> implements ObjectPool<T> {
	
	private final Holder<T>[] pool;
	
	/**
	 * このプールが保持しているvalueはvolatileなため、検索時にメモリバリアによるメモリ同期が発生する。
	 * 検索専用にvolatileではない配列にvalueを保持させ、indexの高速検索を可能とする。
	 * rentedはスレッド間のメモリ可視性により、正しいオブジェクト参照が見えない可能性が在るため、
	 * 実際の検索でヒットしなかった場合にはvolatileなvalueの検索も実施される。
	 */
	private final Object[] rented;
	
	
	@SuppressWarnings("unchecked")
	public ArrayObjectPool(int totalSize) {
		pool = new Holder[totalSize];
		for(int i=0; i < totalSize; i++) {
			pool[i] = new Holder<T>();
		}
		rented = new Object[totalSize];
	}
	
	
	public T getValue(int index) {
		if(pool.length <= index) {
			throw new RuntimeException("bad operation totalSize=" + pool.length + ", index=" + index);
		}
		return pool[index].value;
	}
	
	public void setValue(int index, T value) {
		if(pool.length <= index) {
			throw new RuntimeException("bad operation totalSize=" + pool.length + ", index=" + index);
		}
		
		pool[index].value = value;
		rented[index] = value;
	}
	
	public int search(T value) {
		for(int i=0; i<rented.length; i++) {
			if(rented[i] == value) {
				return i;
			}
		}
		// JMMの可視性により、non volatileな検索で検索出来ない場合がある。
		//  volatileな検索を実施して、確実にチェックする
		return searchPool(value);
	}

	private int searchPool(T value) {
		for(int i=0; i<pool.length; i++) {
			if(pool[i].value == value) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static class Holder<T> {
		volatile T value;
	}
}
