import com.worldpay.context.BuildContext
import com.worldpay.utils.TokenHelper

/**
 *
 * Runs the helm deployment stage and detects if it should use funtional environments
 * Usage:
 * withHelmDeploymentDynamicStage {
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
