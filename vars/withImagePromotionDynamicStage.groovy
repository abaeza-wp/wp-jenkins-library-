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

                // If a source namespace is not provided then we assume we are promoting from a namespace to the same namespace in another environment
                def sourceNamespaceValue = (sourceNamespace != null) ? sourceNamespace : "${namespace}-${functionalEnvironment}"

                // If a sourceCredentialId is not provided then we assume we are promoting from a namespace to the same namespace in another environment
                def sourceCredentialIdValue = (sourceCredentialId != null) ? sourceCredentialId : TokenHelper.tokenNameOf(sourceEnvironment, destinationNamespace, awsRegion)

                //Obtain tokens
                def sourceRegistryToken = kubernetesLogin(sourceProfile.cluster.api, clusterUsername, sourceCredentialIdValue, sourceNamespaceValue, false)
                def destinationRegistryToken = kubernetesLogin(destinationProfile.cluster.api, null, destinationCredentialId, destinationNamespace, false)

                promoteImageFromTo(
                        sourceNamespaceValue,
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

            // If a source namespace is not provided then we assume we are promoting from a namespace to the same namespace in another environment
            def sourceNamespaceValue = (sourceNamespace != null) ? sourceNamespace : "${namespace}"

            // If a sourceCredentialId is not provided then we assume we are promoting from a namespace to the same namespace in another environment
            def sourceCredentialIdValue = (sourceCredentialId != null) ? sourceCredentialId : TokenHelper.tokenNameOf(environmentName, destinationNamespace, awsRegion)

            //Obtain tokens
            def sourceRegistryToken = kubernetesLogin(sourceProfile.cluster.api, clusterUsername, sourceCredentialIdValue, sourceNamespaceValue, false)
            def destinationRegistryToken = kubernetesLogin(destinationProfile.cluster.api, null, destinationCredentialId, destinationNamespace, false)

            promoteImageFromTo(
                    sourceNamespaceValue,
                    sourceRegistryToken,
                    sourceRegistry,
                    destinationNamespace,
                    destinationRegistryToken,
                    destinationRegistry)
        }
    }
}
