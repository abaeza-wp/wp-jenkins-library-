import com.worldpay.context.BuildContext

/*
 Used to scan the built container image using Sysdig Secure, for the purpose of image dependency vulnerability
 management and best practices.
 */

def call(String imageNamespace, String clusterUsername) {
    withCredentials([
        string(credentialsId: "${env.SYSDIG_IMAGE_SCANNING_API_CREDENTIAL_ID}", variable: "SYSDIG_API_KEY")
    ]) {
        echo "Running Sysdig scan..."
        def hasSysdigScanPassed = false
        def resultsUrl = ""

        def kubernetesToken = kubernetesLogin(clusterUsername, "${env.SVC_TOKEN}", imageNamespace)

        registry = BuildContext.currentBuildProfile.cluster.imageRegistry
        namespace = "${imageNamespace}"
        imageName = "${env.SERVICE_NAME}"
        imageTag = "${env.BUILD_APP_VERSION}"

        imageUrl = "${registry}/${namespace}/${imageName}:${imageTag}"
        echo "Image URL: ${imageUrl}"

        try {
            echo "Executing sysdig scan..."
            sh """
                SECURE_API_TOKEN=${SYSDIG_API_KEY} \
                REGISTRY_USER=${clusterUsername} \
                REGISTRY_PASSWORD=${kubernetesToken} \
                \
                sysdig-cli-scanner \
                    --apiurl=https://secure.sysdig.com ${imageUrl} \
                    --loglevel=debug \
                    --console-log \
                    --output-json=./scan-result.json \
                    --dbpath=/tmp/ \
                    --skiptlsverify
            """

            sh """
                mv scan-result.json ${env.SERVICE_NAME}-sysdig-scan-result.json
            """
            archiveArtifacts artifacts: "${env.SERVICE_NAME}-sysdig-scan-result.json"

            echo "Checking status.policy in the sysdig report"
            def scanOutput = readJSON file: "./${env.SERVICE_NAME}-sysdig-scan-result.json"

            hasSysdigScanPassed = scanOutput.policies.status == "accepted" || scanOutput.policies.status == "passed"
            echo "Report URL: ${scanOutput.info.resultURL}"
            resultsUrl = scanOutput.info.resultURL

            if (!hasSysdigScanPassed) {
                unstable("Sysdig scan failed - policy violation")
            }
        }
        catch (err) {
            echo "Caught: ${err}"
            unstable("Sysdig scan failed")
        }
        finally {
            sendSlackNotificationSysdig(hasSysdigScanPassed, resultsUrl)
        }
    }
}
