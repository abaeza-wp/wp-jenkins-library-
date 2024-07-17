import com.worldpay.context.BuildContext
import com.worldpay.utils.TokenHelper

def call(String sourceEnvironment, String destinationEnvironment) {
    call(sourceEnvironment, destinationEnvironment, null, null, null)
}

def call(String sourceEnvironment, String destinationEnvironment, String clusterUsername, String sourceCredentialId, String sourceNamespace) {
    def environmentName = BuildContext.currentBuildProfile.cluster.environment
    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion
    def namespace = BuildContext.fullName

    if (BuildContext.useFunctionalEnvironments) {
        for (functionalEnvironment in BuildContext.functionalEnvironments) {
            stage("[${environmentName}] [${functionalEnvironment}] Promote Image") {
                def destinationNamespace = "${namespace}-${functionalEnvironment}"

                def destinationCredentialId = TokenHelper.tokenNameOf(environmentName, destinationNamespace, awsRegion)

                def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
                def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

                def sourceRegistry = sourceProfile.cluster.imageRegistry
                def destinationRegistry = destinationProfile.cluster.imageRegistry

                if (sourceNamespace == null) {
                    // If a source namespace is not provided then we assume we are promoting from a namespace to the same namespace in another environment
                    sourceNamespace = "${namespace}-${functionalEnvironment}"
                }
                echo "Source credentialId: ${sourceCredentialId}"
                echo "Source credential: ${destinationCredentialId}"
                //Obtain tokens
                def sourceRegistryToken = kubernetesLogin(sourceProfile.cluster.api, clusterUsername, sourceCredentialId, sourceNamespace, false)
                def destinationRegistryToken = kubernetesLogin(destinationProfile.cluster.api, null, destinationCredentialId, destinationNamespace, false)

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
        stage("[${environmentName}] Promote Image") {

            def destinationCredentialId = TokenHelper.tokenNameOf(environmentName, namespace, awsRegion)

            def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
            def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

            def destinationNamespace = namespace
            def sourceRegistry = sourceProfile.cluster.imageRegistry
            def destinationRegistry = destinationProfile.cluster.imageRegistry

            if (sourceNamespace == null) {
                // If a source namespace is not provided then we assume we are promoting from a namespace to the same namespace in another environment
                sourceNamespace = "${namespace}"
            }

            //Obtain tokens
            def sourceRegistryToken = kubernetesLogin(sourceProfile.cluster.api, clusterUsername, sourceCredentialId, sourceNamespace, false)
            def destinationRegistryToken = kubernetesLogin(destinationProfile.cluster.api, null, destinationCredentialId, destinationNamespace, false)

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
