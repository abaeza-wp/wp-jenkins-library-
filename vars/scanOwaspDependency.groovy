/*
    Used to scan the application's dependencies, using the OWASP Dependency Checker Gradle plugin, for the purpose
    of dependency vulnerability management.
*/

def call() {

    // Pull NVD files from s3
    withCredentials([[$class: "AmazonWebServicesCredentialsBinding", accessKeyVariable: "AWS_ACCESS_KEY_ID", credentialsId: "${env.OWASP_DEPENDENCY_NVD_BUCKET_CREDENTIAL_ID}", secretKeyVariable: "AWS_SECRET_ACCESS_KEY"]])
    {
        sh """
            s3cmd get --recursive s3://${env.OWASP_DEPENDENCY_NVD_BUCKET_NAME}/nvd .
        """
    }

    // Run OWASP dependency checker
    catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE')
    {
        sh """
            ./gradlew ${env.SERVICE_NAME}:dependencyCheckAnalyze
        """
    }
    archiveReportAsPdf("OWASP Dependency Checker", "${env.SERVICE_NAME}/build/reports", "dependency-check-report.html", "owasp-report.pdf", false)
}
