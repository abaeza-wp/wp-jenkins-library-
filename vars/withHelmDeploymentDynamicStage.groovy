import com.worldpay.context.BuildContext
import com.worldpay.context.GkopCluster
import com.worldpay.utils.TokenHelper

/**
 *
 * Runs the helm deployment stage and detects if it should use functional environments
 * Usage:
 * withHelmDeploymentDynamicStage {
 *   ...
 * }
 */

def call(Map parameters) {
    def TARGET_CLUSTER = parameters.cluster as GkopCluster
    def TARGET_CLUSTER_USERNAME = parameters.clusterUsername as String
    def TARGET_NAMESPACE = parameters.clusterCredentialId as String

    def environmentName = TARGET_CLUSTER.environment
    def awsRegion = TARGET_CLUSTER.awsRegion

    if (BuildContext.useFunctionalEnvironments) {
        for (functionalEnvironment in BuildContext.functionalEnvironments) {
            stage("[${environmentName}] [${functionalEnvironment}] Deploy Application") {
                def destinationNamespace = "${TARGET_NAMESPACE}-${functionalEnvironment}"
                def clusterCredentialId = TokenHelper.tokenNameOf(environmentName, destinationNamespace, awsRegion)
                helmDeployment(
                targetCluster: TARGET_CLUSTER,
                username: TARGET_CLUSTER_USERNAME,
                credentialId: clusterCredentialId,
                namespace: destinationNamespace,
                functionalEnvironment: "${functionalEnvironment}",
                )
            }
        }
    } else {
        stage("[${environmentName}] Deploy Application") {
            def clusterCredentialId = TokenHelper.tokenNameOf(environmentName, TARGET_NAMESPACE, awsRegion)
            helmDeployment(
            targetCluster: TARGET_CLUSTER,
            username: TARGET_CLUSTER_USERNAME,
            credentialId: clusterCredentialId,
            namespace: TARGET_NAMESPACE,
            )
        }
    }
}
