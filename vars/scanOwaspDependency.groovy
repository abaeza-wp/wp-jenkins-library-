/*
 Used to scan the application's dependencies, using the OWASP Dependency Checker Gradle plugin, for the purpose
 of dependency vulnerability management.
 */

def call(Map parameters) {
	def SERVICE_NAME = parameters.serviceName
	def OWASP_DEPENDENCY_NVD_BUCKET_CREDENTIAL_ID = parameters.owaspCredentialId
	def OWASP_DEPENDENCY_NVD_BUCKET_NAME = parameters.owaspNvdBucketName
	// Pull NVD files from s3
	withCredentials([
		[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${OWASP_DEPENDENCY_NVD_BUCKET_CREDENTIAL_ID}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
	]) {
		sh """
            s3cmd get --recursive s3://${OWASP_DEPENDENCY_NVD_BUCKET_NAME}/nvd .
        """
	}

	// Run OWASP dependency checker
	catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
		sh """
            ./gradlew ${SERVICE_NAME}:dependencyCheckAnalyze
        """
	}
	archiveReportAsPdf('OWASP Dependency Checker', "${SERVICE_NAME}/build/reports", 'dependency-check-report.html', 'owasp-report.pdf', false)
}
