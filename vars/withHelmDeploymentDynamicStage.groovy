import com.worldpay.pipeline.BuildContext
import com.worldpay.pipeline.TokenHelper

/**
 *
 * Runs the block of code if the current configuration defines functional environments
 * Usage:
 * withFunctionalEnvironments {
 *   ...
 * }
 */

def call() {
    def environmentName = BuildContext.currentBuildProfile.cluster.environment
    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion

    def stageName = environmentName != null ? "[${environmentName}] Deploy Application" : "Deploy Application"
    if (BuildContext.useFunctionalEnvironments) {
        for (fEnv in BuildContext.functionalEnvironments) {
            stage("${stageName} [${fEnv}]") {
                environment {
                    SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.fullName, awsRegion, fEnv)
                }
                helmDeployment("${fEnv}")
            }
        }
    } else {
        stage("${stageName}") {
            environment {
                SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.fullName, awsRegion)
            }
            helmDeployment()
        }
    }
}
