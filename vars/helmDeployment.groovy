import com.worldpay.pipeline.BuildConfigurationMapper

/*
 Used to update the Kubernetes resources, in all environments (including production).
 */

def call() {

    def chartLocation = "./charts/${env.FULL_APP_NAME}"
    def appVersion = "${env.BUILD_APP_VERSION}"

    def awsRegion = BuildConfigurationMapper.currentBuildConfig.cluster.awsRegion
    def environment = BuildConfigurationMapper.currentBuildConfig.cluster.environment
    def clusterName = BuildConfigurationMapper.currentBuildConfig.cluster.clusterName

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
