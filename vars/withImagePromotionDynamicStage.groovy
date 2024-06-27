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

def call(String sourceEnvironment, String destinationEnvironment) {
    def environmentName = BuildContext.currentBuildProfile.cluster.environment
    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion

    def stageName = environmentName != null ? "[${environmentName}] Promote Image" : "Promote Image"
    if (BuildContext.useFunctionalEnvironments) {
        for (fEnv in BuildContext.functionalEnvironments) {
            stage("${stageName} [${fEnv}]") {
                environment {
                    FROM_SVC_TOKEN = TokenHelper.tokenNameOf(sourceEnvironment, BuildContext.fullName, awsRegion, fEnv)
                    TO_SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.fullName, awsRegion, fEnv)
                }
                promoteImage(sourceEnvironment, destinationEnvironment, "${fEnv}", awsRegion)
            }
        }
    } else {
        stage("${stageName}") {
            environment {
                FROM_SVC_TOKEN = TokenHelper.tokenNameOf(sourceEnvironment, BuildContext.fullName, awsRegion,)
                TO_SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.fullName, awsRegion)
            }
            promoteImage(sourceEnvironment, destinationEnvironment, awsRegion)
        }
    }
}
