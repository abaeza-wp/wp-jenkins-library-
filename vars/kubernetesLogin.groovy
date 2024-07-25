import com.worldpay.context.BuildContext
import com.worldpay.context.GkopCluster

/*
 Used to login to a Kubernetes (GKOP) cluster, and provide a temporary short-lived Kubernetes token (for usage with
 CLI tools).
 This will also login the current build agent to the target Kubernetes cluster, so that the OpenShift client
 (oc command) can be used.
 */

//def call(String clusterApi, String clusterUsername, String credentialId, String namespace, Boolean ignoreTls) {
def call(Map parameters) {
	def CLUSTER = parameters.cluster as GkopCluster
	def USERNAME = parameters.username as String
	def PASSWORD_CREDENTIAL_ID = parameters.passwordCredentialId
	def NAMESPACE = parameters.namespace

	withCredentials([
		string(credentialsId: PASSWORD_CREDENTIAL_ID, variable: 'JENKINS_PASSWORD')
	]) {
		echo "Logging into cluster using credentialId: ${PASSWORD_CREDENTIAL_ID} ..."

		// Implementation Note: Due to Groovy string interpolation secrets may be printed out therefore we use double
		// quotes and escape the $ sign for secret values.
		//
		// This then uses groovy string interpolation for the non secret values but does not for the ignored $ symbols
		// See: https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#string-interpolation
		if (USERNAME != null) {
			sh "oc login ${CLUSTER.api} --username=${USERNAME} --password=\$JENKINS_PASSWORD"
		} else {
			sh "oc login ${CLUSTER.api} --token=\$JENKINS_PASSWORD"
		}

		if (NAMESPACE) {
			// Set namespace for service (fail-safe) - allowed to fail as may not exist yet
			sh "oc project ${NAMESPACE} || true"
		}

		kubernetesToken = sh(script: 'oc whoami -t', returnStdout: true).trim()
		return kubernetesToken
	}
}
