def call(String type, String tenant, String component, Closure body) {
/*
    Used to build the container image.

    Scroll further down to the executeImageBuild function to customise the built steps.
*/

    script
    {
        echo "Running from Shared pipeline"
        // Fetch versioning information
        def gitReference = (env.BRANCH_NAME ? env.BRANCH_NAME : params.gitReference)
        def versionName = load("deployment/boilerplate/scripts/get-version.groovy").getVersion()

        // Set job title
        currentBuild.displayName = "#${currentBuild.number} : ${params.profile} : ${gitReference} : ${versionName}"

        // Read deployment profile
        def profile = readYaml(file: "deployment/profiles/${params.profile}.yml")

        // Login to the cluster
        def kubernetesToken = load("deployment/boilerplate/scripts/kubernetes-login.groovy").kubernetesLogin()

        // Create Kubernetes namespace (dev cluster only)
        if (isCreateNamespace(profile)) {
            load("deployment/boilerplate/scripts/create-dev-namespace.groovy").createDevNamespace(profile)
        }

        // Build the project, as well as the image using Google Jib
        executeImageBuild(profile, kubernetesToken, versionName)
    }
    body.call()
}


/*
    Used to execute the container build.

    If you really need to deviate with building the image, change the function below...
*/

def executeImageBuild(profile, kubernetesToken, versionName) {
    def profiles = ""

    if (params.release) {
        profiles += "-Prelease";
    }

    /**
     This is needed to set the container creation date to the last commit date.
     without doing this, Jib sets the creation time to Unix epoch (00:00:00, January 1st, 1970 in UTC) and
     might breaks the behaviour when the file mod timestamp gets used for caching purpose.
     For more info see https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago
     */
    def gitCommitTimestamp = sh(script: 'git show -s --format=%cI HEAD', returnStdout: true).trim()

    sh """
            ./gradlew ${env.SERVICE_NAME}:clean ${env.SERVICE_NAME}:build ${env.SERVICE_NAME}:jib \
                -Djib.to.image=${profile.build.docker_registry}/${profile.deploy.namespace}/${env.SERVICE_NAME}:${versionName} \
                -Djib.to.auth.username=${profile.deploy.ocp_username} \
                -Djib.to.auth.password=${kubernetesToken} \
                -Djib.container.creationTime=${gitCommitTimestamp} \
                -Djib.container.filesModificationTime=${gitCommitTimestamp} \
                ${profiles}
        """
}

def isCreateNamespace(profile) {
    return profile.deploy.create_namespace != null &&
    profile.deploy.create_namespace.enabled != null &&
    profile.deploy.create_namespace.enabled;
}

return this
