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
    def environmentName = BuildContext.getCurrentBuildProfile().getCluster().getEnvironment()
    def awsRegion = BuildContext.getCurrentBuildProfile().getCluster().getAwsRegion()

    def stageName = environmentName != null ? "[${environmentName}] Deploy Application" : "Deploy Application"
    if (BuildContext.shouldUseFunctionalEnvironments()) {
        for (fEnv in BuildContext.getFunctionalEnvironments()) {
            stage("${stageName} [${fEnv}]") {
                environment {
                    SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.getFullName(), awsRegion, fEnv)
                }
                helmDeployment("${fEnv}")
            }
        }
    } else {
        stage("${stageName}") {
            environment {
                SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.getFullName(), awsRegion)
            }
            helmDeployment()
        }
    }
}
