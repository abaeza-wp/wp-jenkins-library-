import com.worldpay.context.BuildContext

def call(String sourceEnvironment, String destinationEnvironment, String awsRegion) {
    call(sourceEnvironment, destinationEnvironment, awsRegion, null)
}

def call(String sourceEnvironment, String destinationEnvironment, String awsRegion, String functionalEnvironment) {
    def namespace = "${env.NAMESPACE}"

    if (functionalEnvironment != null) {
        namespace = "${namespace}-${functionalEnvironment}"
    }

    //When promoting an image the namespace should be the same
    def sourceNamespace = namespace
    def destinationNamespace = namespace
    def imageName = "${env.SERVICE_NAME}"
    def imageTag = "${env.BUILD_APP_VERSION}"

    //Detect if using functional environments
    promoteImage(sourceEnvironment, sourceNamespace, destinationEnvironment, destinationNamespace, imageName, imageTag, awsRegion)
}


def promoteImage(String sourceEnvironment, String sourceNamespace, String destinationEnvironment, String destinationNamespace, String imageName, String imageTag, String awsRegion) {
    script
    {
        echo "Will attempt to promote image '${imageName}:${imageTag}' from '${sourceNamespace}' in '${sourceEnvironment}' to '${destinationNamespace}' in '${destinationEnvironment}'"

        def sourceProfile = BuildContext.getBuildProfileForAwsRegion(sourceEnvironment, awsRegion)
        def destinationProfile = BuildContext.getBuildProfileForAwsRegion(destinationEnvironment, awsRegion)

        //Obtain tokens
        def sourceRegistryToken = kubernetesLogin(clusterUsername: null, clusterApi: sourceProfile.cluster.api, jenkinsCredentialId: $ { env.FROM_SVC_TOKEN }, namespace: sourceNamespace, ignoreTls: false)
        def destinationRegistryToken = kubernetesLogin(clusterUsername: null, clusterApi: destinationProfile.cluster.api, jenkinsCredentialId: $ { env.TO_SVC_TOKEN }, namespace: destinationNamespace, ignoreTls: false)

        def sourceRegistry = sourceProfile.cluster.imageRegistry
        def destinationRegistry = destinationProfile.cluster.imageRegistry
        // Logging to registries
        login("ocp", sourceRegistryToken, sourceRegistry)
        login("ocp", destinationRegistryToken, destinationRegistry)

        // Copying image
        // Note: a promotion should be copying from one registry to another one without changing any namespaces or names
        // therefore we can use the same image name and namespace from the config profile, this is done to be able to keep a history of changes and be able to track changes easier
        copyImage(
        "$sourceRegistry/$sourceNamespace/$imageName:$imageTag",
        "$destinationRegistry/$destinationNamespace/$imageName:$imageTag"
        )

        //Logout of all repositories
        logout()
    }
}

def copyImage(srcImage, destImage) {
    script
    {
        copyImage(srcImage, null, destImage, null)
    }
}

/**
 * Used to copy an image from a source registry into another registry, typically used to promote an image to another repository
 * @param srcImage The source image to pull from.
 * @param srcToken The token to use to authenticate against the source repository. Can be null, if null is used no 'src-registry-token' argument is added to skopeo, useful when a registry is public.
 * @param destImage The destination where the image should be copied to.
 * @param destToken The token to use to authenticate against the destination repository. Can be null, if null is used no 'dest-registry-token' argument is added to skopeo, useful when a registry is public.
 */
def copyImage(srcImage, srcToken, destImage, destToken) {
    script
    {
        echo "Copying Image from source repository to destination"

        def args = ""
        if (srcToken != null) {
            args += "--src-registry-token=\"${srcToken}\""
        }
        if (destToken != null) {
            args += "--dest-registry-token=\"${destToken}\""
        }

        // Setup deployment resources
        sh """
             export REGISTRY_AUTH_FILE=~/auth.json
             skopeo --debug copy ${args} docker://${srcImage} docker://${destImage}
            """
    }
}

def login(username, password, registry) {
    script
    {
        echo "Attempting to login to registry '${registry}'"
        sh """
              export REGISTRY_AUTH_FILE=~/auth.json
              echo ${password} | skopeo login -u ${username} --password-stdin ${registry}
            """
    }
}

def logout() {
    script
    {
        echo "Attempting to logout from all registries"
        sh """
             export REGISTRY_AUTH_FILE=~/auth.json
             skopeo logout --all
            """
    }
}

return this

