package com.worldpay

abstract class CommonPipelineDsl implements Serializable {
	final PipelineCallbacksConfig callbacks
	def final steps

	CommonPipelineDsl(Object steps, PipelineCallbacksConfig callbacks) {
		this.callbacks = callbacks
		this.steps = steps
	}

	void before(String stage, Closure body) {
		callbacks.registerBefore(stage, body)
	}

	void after(String stage, Closure body) {
		callbacks.registerAfter(stage, body)
	}

	void afterSuccess(String stage, Closure body) {
		callbacks.registerAfter(stage, 'success', body)
	}

	void afterFailure(String stage, Closure body) {
		callbacks.registerAfter(stage, 'failure', body)
	}

	void afterAlways(String stage, Closure body) {
		callbacks.registerAfter(stage, 'always', body)
	}

	void onStageFailure(Closure body) {
		callbacks.registerOnStageFailure(body)
	}

	void onFailure(Closure body) {
		callbacks.registerOnFailure(body)
	}

	void onSuccess(Closure body) {
		callbacks.registerOnSuccess(body)
	}
}
