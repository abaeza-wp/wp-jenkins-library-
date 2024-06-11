package com.worldpay

class AppPipelineDsl extends CommonPipelineDsl implements Serializable {
	def final steps

	AppPipelineDsl(Object steps, PipelineCallbacksConfig callbacks) {
		super(steps, callbacks)
		this.steps = steps
	}
}
