import com.worldpay.context.BuildContext

def getAwsRegions() {
    return [
    "eu-west-1",
    "us-east-1",
    ]
}

def call() {
    pipeline {
        options {
            ansiColor('xterm')
        }
        agent
        {
            kubernetes
            {
                label "cd-agent"
                defaultContainer "cd-agent"
            }
        }

        parameters {
            string(name: "imageTag",
            defaultValue: "",
            description: "The image tag to deploy")
            choice(
            name: "awsRegion",
            choices: getAwsRegions(),
            description: "The target deployment aws region."
            )
        }

        environment {
            // Read Jenkins configuration
            config = readYaml(file: "deployment/jenkins.yaml")

            // The name of the service
            SERVICE_NAME = "${BuildContext.componentName}"

            //If an image tag was provided use that one
            BUILD_APP_VERSION = "${params.imageTag}"
        }

        stages {
            stage("[stage] Prepare Build Environment") {
//                when {
//                    anyOf {
//                        branch 'master'
//                        branch 'main'
//                    }
//                }
                steps {
                    switchEnvironment("stage", "${params.awsRegion}")
                    setBuildInformation()
                }
            }
            stage("[stage] Promote Image") {
//                when {
//                    anyOf {
//                        branch 'master'
//                        branch 'main'
//                    }
//                }
                steps {
                    script {
                        withImagePromotionDynamicStage("stage", "stage")
                    }
                }
            }
            stage("[stage] Deployment") {
//                when {
//                    allOf {
//                        anyOf {
//                            branch 'master'
//                            branch 'main'
//                        }
//                    }
//                }
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
                    }
                }
            }
            stage("[prod] Prepare Build Environment") {
//                when {
//                    anyOf {
//                        branch 'master'
//                        branch 'main'
//                    }
//                }
                steps {
                    switchEnvironment("prod", "${params.awsRegion}")
                }
            }
            stage("[prod] Promote Image") {
//                when {
//                    anyOf {
//                        branch 'master'
//                        branch 'main'
//                    }
//                }
                steps {
                    script {
                        withImagePromotionDynamicStage("stage", "prod")
                    }
                }
            }
            stage("[prod] Deployment") {
//                when {
//                    anyOf {
//                        branch 'master'
//                        branch 'main'
//                    }
//                }
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
                    }
                }
            }
        }
    }
}
