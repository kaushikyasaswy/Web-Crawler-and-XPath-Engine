package edu.upenn.cis455.crawler;

public class NumberOfFilesCrawled {
	
	private int counter;
	
	/**
	 * An object of this class is used to keep track of the number of files crawled by all the threads. 
	 * Access to this count needs to be synchronized across all the threads.
	 */
	public NumberOfFilesCrawled() {
		counter = 0;
	}
	
	public synchronized void increment() {
		counter++;
	}
	
	public synchronized void decrement() {
		counter--;
	}

	public synchronized int count() {
		return counter;
	}
	
}