import com.worldpay.pipeline.BuildConfigurationMapper

def call(String environment) {
    def currentBuildConfig = BuildConfigurationMapper.getClusterInformationForAwsRegion(environment, "${params.awsRegion}")
    echo """
    Successfully switched environment context to:

			Environment: ${currentBuildConfig.cluster.environment}
			AWS Region: ${currentBuildConfig.cluster.awsRegion}
			Profile Name: ${currentBuildConfig.profileName}
		"""
}
