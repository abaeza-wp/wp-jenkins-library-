import com.worldpay.PipelineRunner

// Override Stage built-in step to include pipeline callbacks
def call(String STAGE_NAME, Closure body) {

    PipelineRunner.getRunner().runWithCallbacks(STAGE_NAME) {
        def res = steps.stage(STAGE_NAME) {
            body.call()
        }
        return res
    }
}
