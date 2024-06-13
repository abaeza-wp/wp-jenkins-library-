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

    echo "Packaging helm release..."
    sh """
            helm package ${chartLocation} --dependency-update --app-version=${appVersion}
        """

    def namespace = "${env.FULL_APP_NAME}-${env.DEPLOYMENT_FUNCTIONAL_ENVIRONMENT}"
    def options = [
    "--set global.awsRegion=${awsRegion}",
    "--set global.environment=${environment}",
    "--set global.clusterName=${clusterName}",
    "--set global.functionalEnvironment=${env.DEPLOYMENT_FUNCTIONAL_ENVIRONMENT}",
    "--set java.imageTag=${appVersion}",
    "--namespace ${namespace}"
    ]

    if (env.IS_PR_BUILD) {
        options += "--set java.fullnameOverride=${env.FULL_APP_NAME}-${env.DEPLOYMENT_FUNCTIONAL_ENVIRONMENT}-${env.BRANCH_NAME}"
    }

    def optionsString = (options + [" --history-max 3 ", "--install", "--wait", "--timeout 120s"]).join(' ')

    echo "Updating Kubernetes resources via Helm..."
    // Install or upgrade via helm
    sh """
            helm upgrade ${env.FULL_APP_NAME} ./${env.FULL_APP_NAME}-1.0.0.tgz ${optionsString} -f ${chartLocation}/values.yaml 
        """
}
