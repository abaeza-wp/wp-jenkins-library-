/*
 Used to update the Kubernetes resources, in all environments (including production).
 */

def call(String profileName) {

	def chartLocation = "./charts/${env.FULL_APP_NAME}"
	def appVersion = "${env.BUILD_APP_VERSION}"

	def deploymentOptions = readYaml(file: "deployment/options.yml")
	def targetConfiguration = deploymentOptions.options[profileName]

	def awsRegion = targetConfiguration.awsRegion
	def environment = targetConfiguration.environment
	def clusterName = targetConfiguration.clusterName

	echo "Packaging helm release..."
	sh """
            helm package ${chartLocation} --dependency-update --app-version=${appVersion}
        """

	echo "Updating Kubernetes resources via Helm..."
	// Install or upgrade via helm
	sh """
            helm upgrade ${env.FULL_APP_NAME} ./${env.FULL_APP_NAME}-1.0.0.tgz \\
            --set global.awsRegion=${awsRegion} \\
            --set global.environment=${environment} \\
            --set global.clusterName=${clusterName} \\
            --set global.functionalEnvironment=${env.DEPLOYMENT_FUNCTIONAL_ENVIRONMENT} \\
            --set java.imageTag=${appVersion} \\
            -f ${chartLocation}/values.yaml --history-max 3 --install --wait --timeout 120s 
        """
}
