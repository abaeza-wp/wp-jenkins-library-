/*
 Used to run performance testing using Gatling.
 */

def call(String profileName) {

	// Initially sleep to give time for the deployment...
	echo "Waiting for deployment..."
	sleep time: env.PERFORMANCE_TESTING_WAIT_SECONDS, unit: 'SECONDS'

	// Wait for the service to become available...
	script {
		def profile = readYaml(file: "deployment/profiles/${profileName}.yml")
		def statusUrl = "https://${profile.deploy.hostname}/status"

		retry(6) {
			echo "Checking deployment ready..."

			def response = sh(script: "curl -sl ${statusUrl} | grep -i OK", returnStdout: true)
			if (!response.contains("OK")) {
				sleep time: 30, unit: 'SECONDS'
				error "Deployment not ready for performance testing, url: ${statusUrl}, response: ${response}"
			}
		}
	}

	// Run Gatling performance tests...
	catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
		script {
			sh """
                ./gatling.sh ${params.profile}
            """
		}
	}

	// Publish the results
	publishPerformanceTest()
}

def publishPerformanceTest() {
	catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
		script {
			// Rename the report folder
			sh """
                mv build/reports/gatling/performance* build/reports/gatling/performance
            """

			// Archive it as a PDF
			def lib = load("deployment/boilerplate/scripts/pipeline/reporting.groovy")
			lib.archiveReportAsPdf("Performance Testing", "build/reports/gatling/performance", "index.html", "performance-tests.pdf", true)
		}
	}
}
