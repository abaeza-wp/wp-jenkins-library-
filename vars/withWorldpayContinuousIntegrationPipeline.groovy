import com.worldpay.AppPipelineDsl
import com.worldpay.PipelineCallbacksConfig
import com.worldpay.PipelineRunner
import com.worldpay.context.BuildContext

/**
 *
 * Main entry point to Shared re-usable pipelines
 * Usage:
 * withWorldpayContinuousIntegrationPipeline() {
 *   ...
 * }
 */
def call(String type, String tenant, String component, Closure body) {
	call(type, tenant, component, null, body)
}

def call(String type, String tenant, String component, List<String> functionalEnvironments, Closure body) {
	BuildContext.initialize(tenant, component, functionalEnvironments)

	def callbacks = new PipelineCallbacksConfig()
	PipelineRunner.runner.setConfig(callbacks)

	def dsl = new AppPipelineDsl(this, callbacks)
	body.delegate = dsl
	body.call() // register pipeline config with callbacks

	dsl.onStageFailure {
		currentBuild.result = 'FAILURE'
	}

	switch (type) {
		case 'java':
			withSpringBootPipeline()
			break
		case 'java-old':
			withSpringBootPipelineExampleServiceMigrated()
			break
		case 'java-example-service':
			withSpringBootPipelineExampleService()
			break
		default:
			error "ERROR: Unsupported pipeline type used '${type}'"
			break
	}
	body.call()
}
