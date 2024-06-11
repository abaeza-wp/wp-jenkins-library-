def call() {
    withCredentials([[$class: "AmazonWebServicesCredentialsBinding", accessKeyVariable: "AWS_ACCESS_KEY_ID", credentialsId: "${env.REPORT_ARCHIVING_BUCKET_CREDENTIAL_ID}", secretKeyVariable: "AWS_SECRET_ACCESS_KEY"]])
    {
        def date = new Date().format("yyyy-MM-dd")
        String[] splitDate = date.split("-")
        def year = splitDate[0]

        def version = load("deployment/boilerplate/scripts/get-version.groovy").getVersion()

        def hash = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
        def tagOrBranch = sh(script: "git describe --contains --all HEAD", returnStdout: true).replaceAll('/', '-').trim()

        sh """
                s3cmd put FILE *.pdf s3://${env.REPORT_ARCHIVING_BUCKET_NAME}/${env.SERVICE_NAME}/${year}/${version}/
            """

        // Archive the sysdig report which is in JSON format
        sh """
                s3cmd put FILE *sysdig-scan-result.json s3://${env.REPORT_ARCHIVING_BUCKET_NAME}/${env.SERVICE_NAME}/${year}/${version}/
            """
    }
}
