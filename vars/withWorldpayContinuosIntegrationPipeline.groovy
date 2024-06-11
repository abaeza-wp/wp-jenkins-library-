import com.worldpay.AppPipelineDsl
import com.worldpay.PipelineCallbacksConfig
import com.worldpay.PipelineRunner

def call(String type, String tenant, String component, Closure body) {

	def callbacks = new PipelineCallbacksConfig()
	PipelineRunner.getRunner().setConfig(callbacks)

	def dsl = new AppPipelineDsl(this, callbacks)
	body.delegate = dsl
	body.call() // register pipeline config with callbacks

	dsl.onStageFailure() {
		currentBuild.result = "FAILURE"
	}

	switch (type) {
		case "java":
			withSpringBootPipeline(
			pipelineCallbacksRunner: callbacksRunner,
			tenant: tenant,
			component: component
			)
			break
		case "java-improved-flow":
			withSpringBootPipelineImprovedFlow(
			pipelineCallbacksRunner: callbacksRunner,
			tenant: tenant,
			component: component
			)
			break
		default:
			error "ERROR: Unsupported pipeline type used '${type}'"
			break
	}
	body.call()
}
