package com.alvinquach.jmeter.sampler.async;

import org.apache.jmeter.samplers.SampleResult;

public class AsyncRequestResult<T> extends SampleResult {

	private static final long serialVersionUID = 5490186259043117683L;
	
	private long originalRequestStartTime;
	
	private T identifier;
	
	public AsyncRequestResult() {
		super();
	}
	
	public AsyncRequestResult(AsyncRequestResult<T> res) {
		super(res);
		identifier = res.identifier;
	}
	
	public AsyncRequestResult(long stamp, long elapsed) {
		super(stamp, elapsed);
	}

	public long getOriginalRequestStartTime() {
		return originalRequestStartTime;
	}

	public void setOriginalRequestStartTime(long originalRequestStartTime) {
		this.originalRequestStartTime = originalRequestStartTime;
	}

	public T getIdentifier() {
		return identifier;
	}

	public void setIdentifier(T identifier) {
		this.identifier = identifier;
	}
	
}
