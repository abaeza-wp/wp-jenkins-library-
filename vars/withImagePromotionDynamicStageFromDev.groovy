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

def call(String sourceEnvironment,String destinationEnvironment, String clusterUsername, String sourceCredentialId, String sourceNamespace) {
    def environmentName = BuildContext.currentBuildProfile.cluster.environment
    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion

    def stageName = environmentName != null ? "[${environmentName}] Promote Image" : "Promote Image"
    if (BuildContext.useFunctionalEnvironments) {
        for (functionalEnvironment in BuildContext.functionalEnvironments) {
            stage("${stageName} [${functionalEnvironment}]") {
                environment {
                    DESTINATION_SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.fullName, awsRegion, functionalEnvironment)
                }

                def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
                def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

                def destinationNamespace = "${env.NAMESPACE}-${functionalEnvironment}"

                def sourceRegistry = sourceProfile.cluster.imageRegistry
                def destinationRegistry = destinationProfile.cluster.imageRegistry

                //Obtain tokens
                def sourceRegistryToken = kubernetesLogin(clusterUsername, sourceProfile.cluster.api, sourceCredentialId, sourceNamespace, false)
                def destinationRegistryToken = kubernetesLogin( null, destinationProfile.cluster.api, "${env.DESTINATION_SVC_TOKEN}", destinationNamespace, false)

                promoteImageFromTo(
                    sourceNamespace,
                    sourceRegistryToken,
                    sourceRegistry,
                    destinationNamespace,
                    destinationRegistryToken,
                    destinationRegistry)
            }
        }
    } else {
        stage("${stageName}") {
            environment {
                DESTINATION_SVC_TOKEN = TokenHelper.tokenNameOf(environmentName, BuildContext.fullName, awsRegion)
            }

            def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
            def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

            def destinationNamespace = "${env.NAMESPACE}"
            def sourceRegistry = sourceProfile.cluster.imageRegistry
            def destinationRegistry = destinationProfile.cluster.imageRegistry

            //Obtain tokens
            def sourceRegistryToken = kubernetesLogin(clusterUsername, sourceProfile.cluster.api, sourceCredentialId, sourceNamespace, false)
            def destinationRegistryToken = kubernetesLogin( null, destinationProfile.cluster.api, "${env.DESTINATION_SVC_TOKEN}", destinationNamespace, false)

            promoteImageFromTo(
            sourceNamespace,
            sourceRegistryToken,
            sourceRegistry,
            destinationNamespace,
            destinationRegistryToken,
            destinationRegistry)
        }
    }
}
