package com.worldpay

class PipelineRunner implements Serializable {
    private PipelineCallbacksConfig config
    private static PipelineRunner instance

    private PipelineRunner() {
        //Private constructor to prevent instantiation
    }

    static PipelineRunner getRunner() {
        if (instance == null) {
            instance = new PipelineRunner()
        }
        return instance
    }

    void setConfig(PipelineCallbacksConfig config) {
        this.config = config
    }

    void callAfterSuccess(String stage) {
        callIfDefined('after:' + stage + ':success', stage)
    }

    void callAfterFailure(String stage) {
        callIfDefined('after:' + stage + ':failure', stage)
    }

    void callAfterAlways(String stage) {
        callIfDefined('after:' + stage + ':always', stage)
    }

    void callBefore(String stage) {
        callIfDefined('before:' + stage, stage)
    }

    Boolean shouldSkip(String stage) {
        def body = config.bodies.get('skip:' + stage)
        def shouldSkipFound = body != null
        if (shouldSkipFound){
            body.call()
            return true
        }
        return false
    }

    void runWithCallbacks(String stage, Closure body) {
        def errToThrow = null

        if (!shouldSkip(stage)) {
            callBefore(stage)
            try {
                body.call()
                callAfterSuccess(stage)
            } catch (err) {
                call('onStageFailure', stage)

                callAfterFailure(stage)
                throw err
            } finally {
                try {
                    callAfterAlways(stage)
                } catch (err) {
                    call('onStageFailure', stage)
                    errToThrow = err
                }
                callIfDefined('after:all', stage)
                if (errToThrow != null) {
                    throw errToThrow
                }
            }
        }
    }

    void call(String callback, String stage = null) {
        callIfDefined(callback, stage)
    }

    private def callIfDefined(String key, String stage) {
        def body = config.bodies.get(key)
        if (body != null) {
            body.call(stage)
        }
    }
}
