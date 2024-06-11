import com.worldpay.pipeline.BuildConfigurationMapper

def call(String environment) {
    BuildConfigurationMapper.getClusterInformationForAwsRegion(environment, "${params.awsRegion}")
    echo """
			Switched to:
			Environment: ${currentBuildConfig.cluster.environment}
			AWS Region: ${currentBuildConfig.cluster.awsRegion}
			Profile Name: ${currentBuildConfig.profileName}
		"""
}
