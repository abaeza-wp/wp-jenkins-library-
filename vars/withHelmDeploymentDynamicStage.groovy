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
        for (functionalEnvironment in BuildContext.functionalEnvironments) {
            stage("${stageName} [${functionalEnvironment}]") {
                def destinationNamespace = "${namespace}-${functionalEnvironment}"

                def token = TokenHelper.tokenNameOf(environmentName, destinationNamespace, awsRegion)
                helmDeployment("${functionalEnvironment}", destinationNamespace, token)
            }
        }
    } else {
        stage("${stageName}") {
            def token = TokenHelper.tokenNameOf(environmentName, namespace, awsRegion)
            helmDeployment(namespace,token)
        }
    }
}
