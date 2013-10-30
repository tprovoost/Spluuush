package com.tprovoost.spluuush;

public class ThreadUtil {

	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Runs the runnable inside a thread.
	 * @param runnable
	 */
	public static void bgRun(Runnable runnable) {
		new Thread(runnable).start();
	}	
}
