// Override Stages built-in step to include pipeline callbacks
def call(Closure body) {
    script {
        body.call()
    }
}
