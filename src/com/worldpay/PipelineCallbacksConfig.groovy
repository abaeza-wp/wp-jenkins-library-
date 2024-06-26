package com.worldpay

class PipelineCallbacksConfig {
    Map<String, Closure> bodies = new HashMap<>()

    void registerSkip(String stage, Closure body) {
        bodies.put('skip:' + stage, body)
    }

    void registerBefore(String stage, Closure body) {
        bodies.put('before:' + stage, body)
    }

    void registerAfter(String stage, Closure body) {
        bodies.put('after:' + stage, body)
    }

    void registerAfter(String stage, String condition, Closure body) {
        bodies.put('after:' + stage + ':' + condition, body)
    }

    void registerOnStageFailure(Closure body) {
        bodies.put('onStageFailure', body)
    }

    void registerOnFailure(Closure body) {
        bodies.put('onFailure', body)
    }

    void registerOnSuccess(Closure body) {
        bodies.put('onSuccess', body)
    }

    void registerAfterAll(Closure body) {
        bodies.put('after:all', body)
    }
}
