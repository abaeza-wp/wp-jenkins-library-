import com.worldpay.context.BuildContext

/*
 Used to build the container image.
 Scroll further down to the executeImageBuild function to customise the built steps.
 */

def call(Boolean isRelease, String clusterUsername, String namespace, String ignoreTls) {
    def ignoreTlsBoolean = ignoreTls as Boolean
    def kubernetesToken = kubernetesLogin(clusterUsername, namespace, ignoreTlsBoolean)

    def profiles = ""

    if (isRelease) {
        profiles += "-Prelease"
    }

    def profile = BuildContext.currentBuildProfile
    def imageTag = BuildContext.imageTag
    def image = "${profile.cluster.imageRegistry}/${namespace}/${env.SERVICE_NAME}:${imageTag}"

    /**
     * env.GIT_COMMIT_TIMESTAMP
     This is needed to set the container creation date to the last commit date.
     without doing this, Jib sets the creation time to Unix epoch (00:00:00, January 1st, 1970 in UTC) and
     might breaks the behaviour when the file mod timestamp gets used for caching purpose.
     For more info see https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago
     Note: Will only build the image and skip checks usually must run after 'withGradleBuildOnly'
     */
    sh """
            ./gradlew ${env.SERVICE_NAME}:clean ${env.SERVICE_NAME}:build ${env.SERVICE_NAME}:jib -x check \
                -Djib.to.image=${image} \
                -Djib.to.auth.username=${clusterUsername} \
                -Djib.to.auth.password=${kubernetesToken} \
                -Djib.container.creationTime=${env.GIT_COMMIT_TIMESTAMP} \
                -Djib.container.filesModificationTime=${env.GIT_COMMIT_TIMESTAMP} \
                ${profiles}
        """
}
