/*
    Used to scan the application binary using Blackduck, for the purpose of dependency vulnerability and license
    management.
*/

def call(Closure body) {
    script
    {
        def scanDir = "blackduckscan"

        withCredentials([string(credentialsId: "${env.BLACKDUCK_API_CREDENTIAL_ID}", variable: "BLACKDUCK_TOKEN")])
        {
            // JENKINS_NODE_COOKIES=dontKillMe nohup bash ... & is all used to get detect.sh to run in the background.
            // See more here: https://stackoverflow.com/a/37161006/901641
            sh """
                rm -rf ${scanDir}
                mkdir ${scanDir}
                cp ${env.BLACKDUCK_FATJAR} ${scanDir}
                curl -O ${env.BLACKDUCK_DETECT_SCRIPT_URL}
                chmod +x ${env.BLACKDUCK_DETECT_SCRIPT}
                JENKINS_NODE_COOKIE=dontKillMe nohup bash \\
                ./${env.BLACKDUCK_DETECT_SCRIPT} --blackduck.url=${env.BLACKDUCK_URL} \\
                    --blackduck.api.token=${env.BLACKDUCK_TOKEN} --blackduck.trust.cert=true \\
                    --detect.project.name=${env.BLACKDUCK_PROJECT_NAME} \\
                    --detect.project.version.name=${env.BUILD_APP_VERSION} \\
                    --detect.policy.check.fail.on.severities=ALL \\
                    --detect.timeout=900 \\
                    --detect.gradle.excluded.projects=performance-testing \\
                    --detect.gradle.excluded.configurations=bdd*,jacoco*,test*,sonarLint*,spotless* \\
                    --detect.risk.report.pdf=true \\
                    --detect.risk.report.pdf.path=${scanDir} >blackDuckScan.log 2>&1 &
            """
        }

        checkBlackDuckResults()
    }
    body.call()
}


def checkBlackDuckResults() {
    def scanDir = "blackduckscan"
    def hasBlackduckScanPassed = false
    // Check the output file for the scan result for up to 15 minutes (180 * 5 seconds).

    try {
        for (int i = 0; i < 180; i++) {
            def blackduckOutput = readFile(file: "blackDuckScan.log")
            if (!blackduckOutput.contains("Result code of")) {
                sh "sleep 5"
                continue
            }

            // Dump output from Blackduck
            echo "Blackduck output:"
            echo blackduckOutput

            // Archive the PDF
            if (params.release) {
                sh """
                mv ${scanDir}/*.pdf ${env.SERVICE_NAME}-blackduck-report.pdf
            """

                // Archive artifacts
                archiveArtifacts artifacts: "${env.SERVICE_NAME}-blackduck-report.pdf"
            }

            hasBlackduckScanPassed = blackduckOutput.contains("Overall Status: SUCCESS")

            // Mark the build based on the result
            if (hasBlackduckScanPassed) {
                echo "Black Duck scan appears to be good!"

                return
            } else {
                def status = blackduckOutput.substring(blackduckOutput.indexOf("Overall Status:"))
                status = status.substring(0, status.indexOf("-"))

                error "Black Duck scan failed with ${status}. Check the logs and/or the report!"
            }
        }
    }
    catch (err) {
        echo readFile(file: "blackDuckScan.log")
        echo "Caught: ${err}"

        error "Black Duck scan seems to be stuck. Check the logs."
    }
    finally {
        load("deployment/boilerplate/scripts/pipeline/slack-notification.groovy").sendSlackNotificationForBlackduckScan(hasBlackduckScanPassed)
    }
}
