/*
    Used to build the container image.

    Scroll further down to the executeImageBuild function to customise the built steps.
*/

def call() {
    // Read deployment profile
    def profile = readYaml(file: "deployment/profiles/${params.profile}.yml")

    // Create Kubernetes namespace (dev cluster only)
    if (isCreateNamespace(profile)) {
        load("deployment/boilerplate/scripts/create-dev-namespace.groovy").createDevNamespace(profile)
    }

    def kubernetesToken = kubernetesLogin(params.profile)

    def profiles = ""

    if (params.release) {
        profiles += "-Prelease"
    }

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
                -Djib.to.image=${profile.build.docker_registry}/${profile.deploy.namespace}/${env.SERVICE_NAME}:${versionName} \
                -Djib.to.auth.username=${profile.deploy.ocp_username} \
                -Djib.to.auth.password=${kubernetesToken} \
                -Djib.container.creationTime=${env.GIT_COMMIT_TIMESTAMP} \
                -Djib.container.filesModificationTime=${env.GIT_COMMIT_TIMESTAMP} \
                ${profiles}
        """
}
