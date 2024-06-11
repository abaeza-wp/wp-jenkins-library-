def call(Closure body) {
    script
    {
        archiveArtifacts artifacts: "${env.SERVICE_NAME}/build/reports/**/*.*"
    }
    body.call()
}

