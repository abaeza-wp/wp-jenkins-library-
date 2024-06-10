def call(reportName, reportDir, htmlReportFile, outputPdf, waitForJS) {
    script
    {
        archiveReportAsPdf(reportName, reportDir, htmlReportFile, outputPdf, waitForJS)
    }
}

/*
    Used to archive artifacts in an AWS S3 bucket, as to retain them for internal audits and compliance (e.g. PCI-DSS).
*/
def archiveReports()
{
    script
    {
        withCredentials([[$class: "AmazonWebServicesCredentialsBinding", accessKeyVariable: "AWS_ACCESS_KEY_ID", credentialsId: "${env.REPORT_ARCHIVING_BUCKET_CREDENTIAL_ID}", secretKeyVariable: "AWS_SECRET_ACCESS_KEY"]])
        {
            def date = new Date().format("yyyy-MM-dd")
            String [] splitDate = date.split("-")
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
}

def archiveHtmlReports() {

}

def publishHtmlReport(reportDir, reportFile, reportName)
{
    publishHTML (target: [
    allowMissing: false,
    alwaysLinkToLastBuild: false,
    keepAll: true,
    reportDir: "${reportDir}",
    reportFiles: "${reportFile}",
    reportName: "${reportName}"
    ])
}

def archiveReportAsPdf(reportName, reportDir, htmlReportFile, outputPdf, waitForJS)
{
    // Publish on Jenkins
    publishHtmlReport(reportDir, htmlReportFile, reportName)

    // Convert HTML report to PDF
    jsParams = ""

    if (waitForJS) {
        jsParams += "--javascript-delay 10000"
    }

    sh """
      google-chrome --headless --disable-gpu --no-sandbox --print-to-pdf=${env.SERVICE_NAME}-${outputPdf} ${reportDir}/${htmlReportFile}
    """

    // Archive artifacts
    archiveArtifacts artifacts: "${env.SERVICE_NAME}-${outputPdf}"
}

def reportCodeCoverage()
{
    archiveReportAsPdf("Code Coverage", "${env.SERVICE_NAME}/build/reports/jacoco/test/html", "index.html", "coverage-report.pdf", false)
}

def reportBDD()
{
    archiveReportAsPdf("BDD", "${env.SERVICE_NAME}/build/reports/tests/bddTest", "index.html", "bdd-report.pdf", true)
}

def reportUnit()
{
    archiveReportAsPdf("Unit", "${env.SERVICE_NAME}/build/reports/tests/test", "index.html", "unit-test-report.pdf", false)
}
