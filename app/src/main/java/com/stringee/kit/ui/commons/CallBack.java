package com.stringee.kit.ui.commons;

public interface CallBack {
	public void start();

	public void doWork(Object... params);

	public void end(Object[] params);
}
