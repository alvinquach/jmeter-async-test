package com.alvinquach.jmeter.sampler.async;

import org.apache.jmeter.samplers.SampleResult;

public abstract class AbstractAsyncSampleResult extends SampleResult {

	private static final long serialVersionUID = -8972265399862850661L;

	private String identifier;
	
	private long originalRequestStartTime;
	
	public AbstractAsyncSampleResult() {
		super();
	}
	
	public AbstractAsyncSampleResult(AbstractAsyncSampleResult res) {
		super(res);
		identifier = res.identifier;
		originalRequestStartTime = res.originalRequestStartTime;
	}
	
	public AbstractAsyncSampleResult(long stamp, long elapsed) {
		super(stamp, elapsed);
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public long getOriginalRequestStartTime() {
		return originalRequestStartTime;
	}

	public void setOriginalRequestStartTime(long originalRequestStartTime) {
		this.originalRequestStartTime = originalRequestStartTime;
	}
	
}
