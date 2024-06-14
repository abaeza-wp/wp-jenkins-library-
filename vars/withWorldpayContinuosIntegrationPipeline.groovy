import com.worldpay.AppPipelineDsl
import com.worldpay.PipelineCallbacksConfig
import com.worldpay.PipelineRunner
import com.worldpay.pipeline.BuildContext

def call(String type, String tenant, String component, Closure body) {
    call(type, tenant, component, null, body)
}

def call(String type, String tenant, String component, List<String> functionalEnvironments, Closure body) {

    BuildContext.initialize(tenant, component, functionalEnvironments)

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
            withSpringBootPipeline()
            break
        case "java-improved-flow":
            withSpringBootPipelineImprovedFlow()
            break
        default:
            error "ERROR: Unsupported pipeline type used '${type}'"
            break
    }
    body.call()
}
