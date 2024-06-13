import com.worldpay.pipeline.BuildConfigurationContext

/*
 Used to update the Kubernetes resources, in all environments (including production).
 */

def call() {

    def chartLocation = "./charts/${env.FULL_APP_NAME}"
    def appVersion = "${env.BUILD_APP_VERSION}"

    def awsRegion = BuildConfigurationContext.currentBuildConfig.cluster.awsRegion
    def environment = BuildConfigurationContext.currentBuildConfig.cluster.environment
    def clusterName = BuildConfigurationContext.currentBuildConfig.cluster.clusterName
    def functionalEnvironment = "${env.DEPLOYMENT_FUNCTIONAL_ENVIRONMENT}"

    echo "Packaging helm release..."
    sh """
            helm package ${chartLocation} --dependency-update --app-version=${appVersion}
        """

    def namespace = "${env.FULL_APP_NAME}-${functionalEnvironment}"
    def options = [
        "--set global.awsRegion=${awsRegion}",
        "--set global.environment=${environment}",
        "--set global.clusterName=${clusterName}",
        "--set global.functionalEnvironment=${functionalEnvironment}",
        "--set java.imageTag=${appVersion}",
        "--namespace ${namespace}"
    ]

    if (env.IS_PR_BUILD) {
        options.add("--set java.fullnameOverride=${env.FULL_APP_NAME}-${functionalEnvironment}-${env.BRANCH_NAME}")
    }

    def optionsString = (options + [
        " --history-max 3 ",
        "--install",
        "--wait",
        "--timeout 120s"
    ]).join(' ')

    def valuesFilesString = getAllValuesFilesIfExist(chartLocation, environment, functionalEnvironment, awsRegion)
    echo "Updating Kubernetes resources via Helm..."
    // Install or upgrade via helm
    sh """
            helm upgrade ${env.FULL_APP_NAME} ./${env.FULL_APP_NAME}-1.0.0.tgz ${optionsString} -f ${valuesFilesString} 
    """
}

String getAllValuesFilesIfExist(String chartLocation, String environment, String functionalEnvironment, String awsRegion) {
    def valuesFilesFound = [
        "${chartLocation}/values.yaml"] //This is the default values file.
    //Support for environment specific values.<env>.yaml e.g values.dev.yaml, values.staging.yaml, values.prod.yaml
    if (fileExists("${chartLocation}/values.${environment}.yaml")) {
        valuesFilesFound.add("${chartLocation}/values.${environment}.yaml")
    }
    //Support for environment specific values.<functionalEnvironment>.yaml e.g values.live.yaml, values.try.yaml
    if (fileExists("${chartLocation}/values.${functionalEnvironment}.yaml")) {
        valuesFilesFound.add("${chartLocation}/values.${functionalEnvironment}.yaml")
    }
    //Support for region specific values.<region>.yaml e.g values.eu-west-1.yaml, values.us-east-1.yaml
    if (fileExists("${chartLocation}/values.${awsRegion}.yaml")) {
        valuesFilesFound.add("${chartLocation}/values.${awsRegion}.yaml")
    }
    //Support for environment and region specific values.<env>.<region>.yaml e.g values.dev.eu-west-1.yaml, values.dev.us-east-1.yaml
    if (fileExists("${chartLocation}/values.${environment}.${awsRegion}.yaml")) {
        valuesFilesFound.add("${chartLocation}/values.${environment}.${awsRegion}.yaml")
    }
    //Support for environment and region specific values.<functionalEnvironment>.<region>.yaml e.g values.live.eu-west-1.yaml, values.try.us-east-1.yaml
    if (fileExists("${chartLocation}/values.${functionalEnvironment}.${awsRegion}.yaml")) {
        valuesFilesFound.add("${chartLocation}/values.${functionalEnvironment}.${awsRegion}.yaml")
    }
    //Support for environment and region specific values.<env>.<functionalEnvironment>.<region>.yaml e.g values.dev.live.eu-west-1.yaml, values.dev.try.us-east-1.yaml
    if (fileExists("${chartLocation}/values.${environment}.${functionalEnvironment}.${awsRegion}.yaml")) {
        valuesFilesFound.add("${chartLocation}/values.${environment}.${functionalEnvironment}.${awsRegion}.yaml")
    }
    return valuesFilesFound.flatten().join(' -f ')
}
