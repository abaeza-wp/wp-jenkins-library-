def call() {
    script
    {
        archiveArtifacts artifacts: "${env.SERVICE_NAME}/build/reports/**/*.*"
    }
}

