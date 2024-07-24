import com.worldpay.context.BuildContext

def call(String environment, String awsRegion) {
    def currentBuildConfig = BuildContext.getBuildProfileForAwsRegion(environment, "${awsRegion}")
    echo """
    Successfully switched environment context to:

            Environment: ${currentBuildConfig.cluster.environment}
            AWS Region: ${currentBuildConfig.cluster.awsRegion}
            Profile Name: ${currentBuildConfig.profileName}
        """
}
