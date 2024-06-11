import com.worldpay.pipeline.BuildConfigurationMapper

def call(String environment) {
	BuildConfigurationMapper.getClusterInformationForAwsRegion(environment, "${params.awsRegion}")
}
