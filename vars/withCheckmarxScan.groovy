/*
    Used to scan the application source-code using Checkmarx, for the purpose of static analysis.
*/

def call(Closure body) {
    script
    {
        def projectName = env.BRANCH_NAME == "master" ? "${env.SERVICE_NAME}_prod" : "${env.SERVICE_NAME}_dev"

        // Run Checkmarx scanner using Jenkins plugin
        step([$class                       : "CxScanBuilder",
              comment                      : "version: ${env.BUILD_APP_VERSION}",
              credentialsId                : "${env.CHECKMARX_API_CREDENTIAL_ID}",
              excludeFolders               : "!*.class, !*.jar, !*.sql, **/.gradle, .gradle, !*.gradle, **/build, **/target, **/src/test, .git, deployment, docs, openapi, **/node_modules, **/nvd, **/blackduckscan, **/native-libs, performance-testing",
              generatePdfReport            : true,
              vulnerabilityThresholdEnabled: true,
              highThreshold                : "0",
              mediumThreshold              : "0",
              lowThreshold                 : "0",
              projectName                  : projectName,
              serverUrl                    : "https://worldpay.checkmarx.net",
              sourceEncoding               : "1",
              useOwnServerCredentials      : true,
              teamPath                     : "${env.CHECKMARX_TEAM_PATH}",
              incremental                  : false,
              preset                       : "100003",
              filterPattern                : "!*.class, !*.jar, !*.war",
              waitForResultsEnabled        : true
        ])

        // Dump the report
        reportCheckmarx()
    }
    body.call()
}

def reportCheckmarx() {
    // Archive the report
    script
    {
        sh """
            mv Checkmarx/Reports/CxSASTReport*.pdf ${env.SERVICE_NAME}-checkmarx-report.pdf
        """

        // Archive artifacts
        archiveArtifacts artifacts: "${env.SERVICE_NAME}-checkmarx-report.pdf"
    }
}
