def call(Map parameters) {
	def SOURCE_REGISTRY = parameters.sourceRegistry as String
	def SOURCE_NAMESPACE = parameters.sourceNamespace as String
	def SOURCE_REGISTRY_TOKEN = parameters.sourceRegistryToken as String
	def DESTINATION_REGISTRY = parameters.destinationRegistry as String
	def DESTINATION_NAMESPACE = parameters.destinationNamespace as String
	def DESTINATION_REGISTRY_TOKEN = parameters. destinationRegistryToken as String
	//When promoting an image the namespace should be the same
	def imageName = "${env.SERVICE_NAME}"
	def imageTag = "${env.BUILD_APP_VERSION}"

	//Detect if using functional environments
	promoteImage(
			SOURCE_NAMESPACE,
			SOURCE_REGISTRY_TOKEN,
			SOURCE_REGISTRY,
			DESTINATION_NAMESPACE,
			DESTINATION_REGISTRY_TOKEN,
			DESTINATION_REGISTRY,
			imageName,
			imageTag)
}

def promoteImage(String sourceNamespace, String sourceRegistryToken, String sourceRegistry, String destinationNamespace, String destinationRegistryToken, String destinationRegistry, String imageName, String imageTag) {
	script {
		def sourceImage = "$sourceRegistry/$sourceNamespace/$imageName:$imageTag"
		def destinationImage = "$destinationRegistry/$destinationNamespace/$imageName:$imageTag"

		echo "Will attempt to promote image '${imageName}:${imageTag}' from '${sourceImage}' to '${destinationImage}'"

		// Logging to registries
		login('ocp', sourceRegistryToken, sourceRegistry)
		login('ocp', destinationRegistryToken, destinationRegistry)

		// Copying image
		copyImage(sourceImage, destinationImage)

		//Logout of all repositories
		logout()
	}
}

def copyImage(srcImage, destImage) {
	script {
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
	script {
		echo 'Copying Image from source repository to destination'

		def args = ''
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
	script {
		echo "Attempting to login to registry '${registry}'"
		sh """
              export REGISTRY_AUTH_FILE=~/auth.json
              echo ${password} | skopeo login -u ${username} --password-stdin ${registry}
            """
	}
}

def logout() {
	script {
		echo 'Attempting to logout from all registries'
		sh '''
             export REGISTRY_AUTH_FILE=~/auth.json
             skopeo logout --all
            '''
	}
}

return this
