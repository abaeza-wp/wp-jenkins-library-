import com.worldpay.pipeline.BuildContext


def call() {
    //convenience call when not using functional environments
    call(null)
}

/*
 Used to update the Kubernetes resources, in all environments (including production).
 */

def call(String functionalEnvironment) {

    def appName = BuildContext.getFullName()
    def chartLocation = "./charts/${appName}"
    def appVersion = "${BuildContext.getImageTag()}"

    def awsRegion = BuildContext.currentBuildProfile.cluster.awsRegion
    def environment = BuildContext.currentBuildProfile.cluster.environment
    def clusterName = BuildContext.currentBuildProfile.cluster.clusterName

    echo "Packaging helm release..."
    sh """
            helm package ${chartLocation} --dependency-update --app-version=${appVersion}
    """

    def options = [
        "--set global.awsRegion=${awsRegion}",
        "--set global.environment=${environment}",
        "--set global.clusterName=${clusterName}",
        "--set java.imageTag=${appVersion}",
    ]

    if (functionalEnvironment != null) {
        options.add("--set global.functionalEnvironment=${functionalEnvironment}")
        options.add("--namespace=${appName}-${functionalEnvironment}")
    }

    if (env.IS_PR_BUILD) {
        if (functionalEnvironment != null) {
            options.add("--set java.fullnameOverride=${appName}-${functionalEnvironment}-${env.BRANCH_NAME}")
            options.add("--namespace=${appName}-${functionalEnvironment}")
        } else {
            options.add("--set java.fullnameOverride=${appName}-${env.BRANCH_NAME}")
            options.add("--namespace=${appName}")
        }
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
            helm upgrade ${appName} ./${appName}-1.0.0.tgz ${optionsString} -f ${valuesFilesString}
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
