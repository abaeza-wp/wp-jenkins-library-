def call() {
    withCredentials([
        [$class: "AmazonWebServicesCredentialsBinding", accessKeyVariable: "AWS_ACCESS_KEY_ID", credentialsId: "${env.REPORT_ARCHIVING_BUCKET_CREDENTIAL_ID}", secretKeyVariable: "AWS_SECRET_ACCESS_KEY"]
    ]) {
        def date = new Date().format("yyyy-MM-dd")
        String[] splitDate = date.split("-")
        def year = splitDate[0]

        sh """
                s3cmd put FILE *.pdf s3://${env.REPORT_ARCHIVING_BUCKET_NAME}/${env.SERVICE_NAME}/${year}/${env.BUILD_APP_VERSION}/
            """

        // Archive the sysdig report which is in JSON format
        sh """
                s3cmd put FILE *sysdig-scan-result.json s3://${env.REPORT_ARCHIVING_BUCKET_NAME}/${env.SERVICE_NAME}/${year}/${env.BUILD_APP_VERSION}/
            """
    }
}
