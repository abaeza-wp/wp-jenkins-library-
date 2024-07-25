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
                        withImagePromotionDynamicStage('stage', 'stage')
                    }
                }
            }
            stage('[stage] Deployment') {
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
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
                        withImagePromotionDynamicStage('stage', 'prod')
                    }
                }
            }
            stage('[prod] Deployment') {
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
                    }
                }
            }
        }
    }
}
