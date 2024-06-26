def call(reportName, reportDir, htmlReportFile, outputPdf, waitForJS) {
    // Publish on Jenkins
    publishHtmlReport(reportDir, htmlReportFile, reportName)

    // Convert HTML report to PDF
    jsParams = ""

    if (waitForJS) {
        jsParams += "--javascript-delay 10000"
    }

    //Set log level to FATAL only to reduce noise
    sh """
        google-chrome --headless --disable-gpu --no-sandbox --print-to-pdf=${env.SERVICE_NAME}-${outputPdf} ${reportDir}/${htmlReportFile}
    """

    // Archive artifacts
    archiveArtifacts artifacts: "${env.SERVICE_NAME}-${outputPdf}"
}

def publishHtmlReport(reportDir, reportFile, reportName) {
    publishHTML(target: [
        allowMissing         : false,
        alwaysLinkToLastBuild: false,
        keepAll              : true,
        reportDir            : "${reportDir}",
        reportFiles          : "${reportFile}",
        reportName           : "${reportName}"
    ])
}
