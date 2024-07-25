import com.worldpay.context.BuildContext
import com.worldpay.context.GkopCluster

/*
 Used to update the Kubernetes resources, in all environments (including production).
 */

def call(Map parameters) {
	def CLUSTER = parameters.functionalEnvironment as GkopCluster
	def CLUSTER_USERNAME = parameters.username as String
	def CLUSTER_PASSWORD = parameters.credentialId as String //Either Token or Password
	def FUNCTIONAL_ENVIRONMENT = parameters.functionalEnvironment as String
	def NAMESPACE = parameters.namespace as String

	//Login to cluster to ensure credentials are valid and that all operations are performed under the correct namespace

	if (CLUSTER.isDev()) {
		//We login via username and password
		kubernetesLogin(
				cluster: CLUSTER,
				username: "${CLUSTER_USERNAME}",
				passwordCredentialId: "${CLUSTER_PASSWORD}",
				namespace: "${NAMESPACE}"
				)
	} else {
		//We login via token
		kubernetesLogin(
				cluster: CLUSTER,
				passwordCredentialId: "${CLUSTER_PASSWORD}",
				namespace: "${NAMESPACE}"
				)
	}

	def appName = BuildContext.fullName
	def releaseName = appName
	//Note release name needs to be max 53 chars as per Helm validation https://github.
	// com/helm/helm/blob/ff03c66d4475d9daedeee67c18884461441c2e15/pkg/chartutil/validate_name.go#L61
	def chartLocation = "./deployment/charts/${appName}"
	def appVersion = "${BuildContext.imageTag}"

	def awsRegion = CLUSTER.awsRegion
	def environment = CLUSTER.environment
	def clusterName = CLUSTER.clusterName

	echo 'Updating helm dependencies...'
	sh """
            helm dependency update ${chartLocation}
    """

	echo 'Packaging helm release...'
	sh """
            helm package ${chartLocation} --app-version=${appVersion}
    """

	def releasePackageFileName = sh(script: "printf '%s' ${appName}-*.tgz", returnStdout: true).trim()

	echo 'Archiving Helm release...'
	archiveArtifacts artifacts: "${releasePackageFileName}"

	def options = [
		"--set global.awsRegion=${awsRegion}",
		"--set global.environment=${environment}",
		"--set global.clusterName=${clusterName}",
		"--set global.imageTag=${appVersion}",
	] as ArrayList<String>

	if (FUNCTIONAL_ENVIRONMENT != null) {
		options.add("--set global.functionalEnvironment=${FUNCTIONAL_ENVIRONMENT}")
	}

	if (env.IS_PR_BUILD == 'true') {
		//Will append the branch name in the form of "-pr-XXX"
		//env.BRANCH_NAME is only available in multi-branch pipelines and therefore only for PRs
		releaseName += "-${env.BRANCH_NAME}".toLowerCase()
		options.add("--set global.fullnameOverride=${appName}-${env.BRANCH_NAME}")
		// When using a PR deployment replicas are reduced to only 1 instance to avoid consuming all the quotas
		options.add("--set global.replicasOverride=1")

		//On PR Builds we clean up the previous PR deployment before re-installing
		echo 'Cleaning up previous release...'
		//--ignore-not-found is used to make helm succeed in the case where ea previous helm chart was not installed
		sh """
            helm uninstall ${releaseName} --ignore-not-found
        """
	}

	options.add("--set global.namespaceOverride=${NAMESPACE}")
	options.add("--namespace=${NAMESPACE}") //Just for extra safety

	def optionsString = (options + [
		'--history-max 3',
		'--install',
		'--wait',
		'--timeout 300s'
	]).join(' ')

	def valuesFilesString = getAllValuesFilesIfExist(chartLocation, environment, FUNCTIONAL_ENVIRONMENT, awsRegion)

	echo 'Updating Kubernetes resources via Helm...'
	// Install or upgrade via helm
	sh """
        helm upgrade ${releaseName} ./${releasePackageFileName} ${optionsString} -f ${valuesFilesString}
    """
}

String getAllValuesFilesIfExist(String chartLocation, String environment, String functionalEnvironment, String awsRegion) {
	def valuesFilesFound = [
		"${chartLocation}/values.yaml"] //This is the default values file.
	//Support for environment specific values.<env>.yaml e.g values.dev.yaml, values.stage.yaml, values.prod.yaml
	if (fileExists("${chartLocation}/values.${environment}.yaml")) {
		valuesFilesFound.add("${chartLocation}/values.${environment}.yaml")
	}
	//Support for region specific values.<region>.yaml e.g values.eu-west-1.yaml, values.us-east-1.yaml
	if (fileExists("${chartLocation}/values.${awsRegion}.yaml")) {
		valuesFilesFound.add("${chartLocation}/values.${awsRegion}.yaml")
	}
	//Support for environment and region specific values.<env>.<region>.yaml e.g values.dev.eu-west-1.yaml, values.
	// dev.us-east-1.yaml
	if (fileExists("${chartLocation}/values.${environment}.${awsRegion}.yaml")) {
		valuesFilesFound.add("${chartLocation}/values.${environment}.${awsRegion}.yaml")
	}
	if (BuildContext.useFunctionalEnvironments) {
		//Support for environment specific values.<functionalEnvironment>.yaml e.g values.live.yaml, values.try.yaml
		if (fileExists("${chartLocation}/values.${functionalEnvironment}.yaml")) {
			valuesFilesFound.add("${chartLocation}/values.${functionalEnvironment}.yaml")
		}
		//Support for environment and region specific values.<functionalEnvironment>.<region>.yaml e.g values.live.
		// eu-west-1.yaml, values.try.us-east-1.yaml
		if (fileExists("${chartLocation}/values.${functionalEnvironment}.${awsRegion}.yaml")) {
			valuesFilesFound.add("${chartLocation}/values.${functionalEnvironment}.${awsRegion}.yaml")
		}
		//Support for environment and region specific values.<env>.<functionalEnvironment>.<region>.yaml e.g values.
		// dev.live.eu-west-1.yaml, values.dev.try.us-east-1.yaml
		if (fileExists("${chartLocation}/values.${environment}.${functionalEnvironment}.${awsRegion}.yaml")) {
			valuesFilesFound.add("${chartLocation}/values.${environment}.${functionalEnvironment}.${awsRegion}.yaml")
		}
	}
	return valuesFilesFound.flatten().join(' -f ')
}
