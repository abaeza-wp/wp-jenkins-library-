import com.worldpay.context.BuildContext
import com.worldpay.utils.TokenHelper

/**
 *
 * Runs the helm deployment stage and detects if it should use functional environments
 * Usage:
 * withHelmDeploymentDynamicStage {
 *   ...
 * }
 */

def call() {
    def environmentName = BuildContext.currentBuildProfile.cluster.environment
    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion
    def namespace = BuildContext.fullName

    def stageName = environmentName != null ? "[${environmentName}] Deploy Application" : "Deploy Application"
    if (BuildContext.useFunctionalEnvironments) {
        for (fEnv in BuildContext.functionalEnvironments) {
            stage("${stageName} [${fEnv}]") {
                namespace = "${namespace}-${fEnv}"

                def token = TokenHelper.tokenNameOf(environmentName, namespace, awsRegion, fEnv)
                helmDeployment("${fEnv}", token)
            }
        }
    } else {
        stage("${stageName}") {
            def token = TokenHelper.tokenNameOf(environmentName, "${env.NAMESPACE}", awsRegion)
            helmDeployment(token)
        }
    }
}
