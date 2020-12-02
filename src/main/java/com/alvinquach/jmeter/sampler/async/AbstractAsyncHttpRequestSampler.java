package com.alvinquach.jmeter.sampler.async;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import com.alvinquach.jmeter.sampler.AbstractCustomHttpSampler;

public abstract class AbstractAsyncHttpRequestSampler<T extends AbstractAsyncSampleResult> extends AbstractCustomHttpSampler<T>  {

	@Override
	public T runTest(JavaSamplerContext context) {
		T result = super.runTest(context);
		result.setOriginalRequestStartTime(result.getStartTime());
		return result;
	}
	
}
