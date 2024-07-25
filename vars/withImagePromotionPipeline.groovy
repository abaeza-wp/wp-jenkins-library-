import com.worldpay.context.BuildContext

def getAwsRegions() {
	return [
		'eu-west-1',
		'us-east-1',
	]
}

def call() {
	pipeline {
		agent {
			kubernetes {
				label 'cd-agent'
				defaultContainer 'cd-agent'
			}
		}

		parameters {
			string(
					name: 'imageTag',
					defaultValue: '',
					description: 'The image tag to deploy')
			choice(
					name: 'awsRegion',
					choices: getAwsRegions(),
					description: 'The target deployment aws region.'
					)
		}

		environment {
			// The name of the service
			SERVICE_NAME = "${BuildContext.componentName}"

			//If an image tag was provided use that one
			BUILD_APP_VERSION = "${params.imageTag}"
		}

		stages {
			stage('[stage] Prepare Build Environment') {
				steps {
					switchEnvironment('stage', "${params.awsRegion}")
					setBuildInformation()
				}
			}
			stage('[stage] Promote Image') {
				steps {
					script {
						def SOURCE_CLUSTER = BuildContext.getBuildProfileForAwsRegion('stage', "${params.awsRegion}")
						def DESTINATION_CLUSTER = BuildContext.getBuildProfileForAwsRegion('stage', "${params.awsRegion}")
						def NAMESPACE = BuildContext.fullName

						withImagePromotionDynamicStage(
								sourceCluster: SOURCE_CLUSTER.cluster,
								destinationCluster: DESTINATION_CLUSTER.cluster,
								destinationNamespace: NAMESPACE

								)
					}
				}
			}
			stage('[stage] Deployment') {
				steps {
					script {
						def PROFILE = BuildContext.getBuildProfileForAwsRegion('stage', "${params.awsRegion}")
						def NAMESPACE = BuildContext.fullName

						withHelmDeploymentDynamicStage(
								targetCluster: PROFILE.cluster,
								namespace: NAMESPACE
								)
					}
				}
			}
			stage('[prod] Prepare Build Environment') {
				steps {
					switchEnvironment('prod', "${params.awsRegion}")
				}
			}
			stage('[prod] Promote Image') {
				steps {
					script {
						def SOURCE_CLUSTER = BuildContext.getBuildProfileForAwsRegion('stage', "${params.awsRegion}")
						def DESTINATION_CLUSTER = BuildContext.getBuildProfileForAwsRegion('prod', "${params.awsRegion}")
						def NAMESPACE = BuildContext.fullName

						withImagePromotionDynamicStage(
								sourceCluster: SOURCE_CLUSTER.cluster,
								destinationCluster: DESTINATION_CLUSTER.cluster,
								destinationNamespace: NAMESPACE,

								)
					}
				}
			}
			stage('[prod] Deployment') {
				steps {
					script {
						def PROFILE = BuildContext.getBuildProfileForAwsRegion('prod', "${params.awsRegion}")
						def NAMESPACE = BuildContext.fullName

						withHelmDeploymentDynamicStage(
								targetCluster: PROFILE.cluster,
								namespace: NAMESPACE
								)
					}
				}
			}
		}
	}
}
