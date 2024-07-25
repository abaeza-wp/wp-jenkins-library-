import com.worldpay.context.BuildContext
import com.worldpay.context.GkopCluster
import com.worldpay.utils.TokenHelper

//def call(String sourceEnvironment, String destinationEnvironment, String clusterUsername, String sourceCredentialId, String sourceNamespace) {
def call(Map parameters) {

	def SOURCE_CLUSTER = parameters.sourceCluster as GkopCluster
	def SOURCE_CLUSTER_USERNAME = parameters.sourceClusterUsername as String
	def SOURCE_CLUSTER_PASSWORD_CREDENTIAL_ID = parameters.sourceClusterPasswordCredentialId as String
	def SOURCE_NAMESPACE = parameters.sourceNamespace as String

	def DESTINATION_CLUSTER = parameters.destinationCluster as GkopCluster
	def DESTINATION_NAMESPACE = parameters.destinationNamespace as String

	def sourceEnvironment = SOURCE_CLUSTER.environment
	def destinationEnvironment = DESTINATION_CLUSTER.environment

	def awsRegion = DESTINATION_CLUSTER.awsRegion


	if (BuildContext.useFunctionalEnvironments) {
		for (functionalEnvironment in BuildContext.functionalEnvironments) {
			stage("[${destinationEnvironment}] [${functionalEnvironment}] Promote Image") {
				def destinationNamespace = "${DESTINATION_NAMESPACE}-${functionalEnvironment}"
				def destinationCredentialId = TokenHelper.tokenNameOf(destinationEnvironment, destinationNamespace, awsRegion)

				// If a source namespace is not provided then we assume we are promoting from a namespace to the same namespace in another environment
				def sourceNamespaceValue = (SOURCE_NAMESPACE != null) ? SOURCE_NAMESPACE : "${DESTINATION_NAMESPACE}-${functionalEnvironment}"

				// If a sourceCredentialId is not provided then we assume we are promoting from a namespace to the same namespace in another environment
				def sourceCredentialIdValue = (SOURCE_CLUSTER_PASSWORD_CREDENTIAL_ID != null) ? SOURCE_CLUSTER_PASSWORD_CREDENTIAL_ID : TokenHelper.tokenNameOf(sourceEnvironment, destinationNamespace, awsRegion)

				//Obtain tokens
				def sourceRegistryToken = kubernetesLogin(
						cluster: SOURCE_CLUSTER,
						username: "${SOURCE_CLUSTER_USERNAME}",
						passwordCredentialId: "${sourceCredentialIdValue}",
						namespace: "${sourceNamespaceValue}")

				def destinationRegistryToken = kubernetesLogin(
						cluster: DESTINATION_CLUSTER,
						passwordCredentialId: "${destinationCredentialId}",
						namespace: "${destinationNamespace}")

				promoteImageFromTo(
						sourceRegistry: SOURCE_CLUSTER.imageRegistry,
						sourceNamespace: sourceNamespaceValue,
						sourceRegistryToken: sourceRegistryToken,
						destinationRegistry: DESTINATION_CLUSTER.imageRegistry,
						destinationNamespace: destinationNamespace,
						destinationRegistryToken: destinationRegistryToken
						)
			}
		}
	} else {
		stage("[${destinationEnvironment}] Promote Image") {
			def destinationCredentialId = TokenHelper.tokenNameOf(destinationEnvironment, DESTINATION_NAMESPACE, awsRegion)

			def destinationNamespace = DESTINATION_NAMESPACE

			// If a source namespace is not provided then we assume we are promoting from a namespace to the same namespace in another environment
			def sourceNamespaceValue = (SOURCE_NAMESPACE != null) ? SOURCE_NAMESPACE : "${DESTINATION_NAMESPACE}"

			// If a sourceCredentialId is not provided then we assume we are promoting from a namespace to the same namespace in another environment
			def sourceCredentialIdValue = (SOURCE_CLUSTER_PASSWORD_CREDENTIAL_ID != null) ? SOURCE_CLUSTER_PASSWORD_CREDENTIAL_ID : TokenHelper.tokenNameOf(destinationEnvironment, destinationNamespace, awsRegion)

			//Obtain tokens
			def sourceRegistryToken = kubernetesLogin(
					cluster: SOURCE_CLUSTER,
					username: "${SOURCE_CLUSTER_USERNAME}",
					passwordCredentialId: "${sourceCredentialIdValue}",
					namespace: "${sourceNamespaceValue}"
					)
			def destinationRegistryToken = kubernetesLogin(
					cluster: DESTINATION_CLUSTER,
					passwordCredentialId: "${destinationCredentialId}",
					namespace: "${destinationNamespace}"
					)
			promoteImageFromTo(
					sourceRegistry: SOURCE_CLUSTER.imageRegistry,
					sourceNamespace: sourceNamespaceValue,
					sourceRegistryToken: sourceRegistryToken,
					destinationRegistry: DESTINATION_CLUSTER.imageRegistry,
					destinationNamespace: destinationNamespace,
					destinationRegistryToken: destinationRegistryToken)
		}
	}
}
