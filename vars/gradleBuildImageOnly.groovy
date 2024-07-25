import com.worldpay.context.BuildContext
import com.worldpay.context.GkopCluster

/*
 Used to build the container image.
 Scroll further down to the executeImageBuild function to customise the built steps.
 This
 */

def call(Map parameters) {
	def CLUSTER = parameters.targetCluster as GkopCluster
	def SERVICE_NAME = parameters.serviceName as String
	def CLUSTER_USERNAME = parameters.clusterUsername as String
	def CLUSTER_CREDENTIAL_ID = parameters.clusterCredentialId as String
	def NAMESPACE = parameters.namespace as String
	def IS_RELEASE = parameters.isRelease as Boolean

	def kubernetesToken = kubernetesLogin(
			cluster: CLUSTER,
			username: "${CLUSTER_USERNAME}",
			passwordCredentialId: "${CLUSTER_CREDENTIAL_ID}",
			namespace: "${NAMESPACE}"
			)
	def profiles = IS_RELEASE ? '-Prelease' : ''

	def profile = BuildContext.currentBuildProfile
	def imageTag = BuildContext.imageTag
	def image = "${profile.cluster.imageRegistry}/${NAMESPACE}/${SERVICE_NAME}:${imageTag}"

	/**
	 * GIT_COMMIT_TIMESTAMP
	 This is needed to set the container creation date to the last commit date.
	 without doing this, Jib sets the creation time to Unix epoch (00:00:00, January 1st, 1970 in UTC) and
	 might breaks the behaviour when the file mod timestamp gets used for caching purpose.
	 For more info see https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.
	 md#why-is-my-image-created-48-years-ago
	 Note: Will only build the image and skip checks usually must run after 'withGradleBuildOnly'
	 */
	def GIT_COMMIT_TIMESTAMP = sh(script: 'git show -s --format=%cI HEAD', returnStdout: true).trim()

	sh """
            ./gradlew ${SERVICE_NAME}:clean ${SERVICE_NAME}:build ${SERVICE_NAME}:jib -x check \
                -Djib.to.image=${image} \
                -Djib.to.auth.username=${CLUSTER_USERNAME} \
                -Djib.to.auth.password=${kubernetesToken} \
                -Djib.container.creationTime=${GIT_COMMIT_TIMESTAMP} \
                -Djib.container.filesModificationTime=${GIT_COMMIT_TIMESTAMP} \
                ${profiles}
        """
}
