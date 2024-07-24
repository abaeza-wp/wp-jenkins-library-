package com.worldpay

class AppPipelineDsl extends CommonPipelineDsl implements Serializable {

    final steps

    AppPipelineDsl(Object steps, PipelineCallbacksConfig callbacks) {
        super(steps, callbacks)
        this.steps = steps
    }

}
