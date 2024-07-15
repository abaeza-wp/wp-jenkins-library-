import com.worldpay.context.BuildContext
import com.worldpay.utils.TokenHelper

def call(String sourceEnvironment, String destinationEnvironment, String clusterUsername, String sourceCredentialId, String sourceNamespace) {
    def environmentName = BuildContext.currentBuildProfile.cluster.environment
    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion
    def namespace = BuildContext.fullName

    if (BuildContext.useFunctionalEnvironments) {
        for (functionalEnvironment in BuildContext.functionalEnvironments) {
            stage("[${environmentName}] [${functionalEnvironment}] Promote Image") {
                namespace = "${namespace}-${functionalEnvironment}"

                def destinationCredentialId = TokenHelper.tokenNameOf(environmentName, BuildContext.componentName, awsRegion, functionalEnvironment)

                def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
                def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

                def destinationNamespace = namespace

                def sourceRegistry = sourceProfile.cluster.imageRegistry
                def destinationRegistry = destinationProfile.cluster.imageRegistry

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

            def destinationCredentialId = TokenHelper.tokenNameOf(environmentName, BuildContext.componentName, awsRegion)

            def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
            def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

            def destinationNamespace = namespace
            def sourceRegistry = sourceProfile.cluster.imageRegistry
            def destinationRegistry = destinationProfile.cluster.imageRegistry

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
