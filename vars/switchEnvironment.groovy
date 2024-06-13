import com.worldpay.pipeline.BuildConfigurationContext

def call(String environment) {
    def currentBuildConfig = BuildConfigurationContext.getClusterInformationForAwsRegion(environment, "${params.awsRegion}")
    echo """
    Successfully switched environment context to:

			Environment: ${currentBuildConfig.cluster.environment}
			AWS Region: ${currentBuildConfig.cluster.awsRegion}
			Profile Name: ${currentBuildConfig.profileName}
		"""
}
