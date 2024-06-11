def call() {
	archiveArtifacts artifacts: "${env.SERVICE_NAME}/build/reports/**/*.*"
}
