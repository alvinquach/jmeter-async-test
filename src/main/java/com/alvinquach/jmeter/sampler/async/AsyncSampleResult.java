package com.alvinquach.jmeter.sampler.async;

public class AsyncSampleResult extends AbstractAsyncSampleResult {

	private static final long serialVersionUID = -2515002474156901793L;
	
	public AsyncSampleResult() {
		super();
	}
	
	public AsyncSampleResult(AsyncSampleResult res) {
		super(res);
	}
	
	public AsyncSampleResult(long stamp, long elapsed) {
		super(stamp, elapsed);
	}
	
}
