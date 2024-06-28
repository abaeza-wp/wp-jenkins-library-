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

            //If namespace is provided we use that one otherwise we use <tenant>-<component-name>
            NAMESPACE = "${config.cd.namespace}"

            //If an image tag was provided use that one
            BUILD_APP_VERSION = "${params.imageTag}"
        }

        stages {
            stage("Prepare Stage Build Environment") {
//                when {
//                    beforeAgent(true)
//                    allOf {
//                        expression { params.release }
//                        anyOf {
//                            branch 'master'
//                            branch 'main'
//                        }
//                    }
//                }
                steps {
                    switchEnvironment("stage", "${params.awsRegion}")
                    setBuildInformation()
                }
            }
            stage("[Stage] Promote Image") {
                steps {
                    script {
                        withImagePromotionDynamicStage("stage", "stage", "${params.imageTag}")
                    }
                }
            }
            stage("[Stage] Deployment") {
//                when {
//                    beforeAgent(true)
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
            stage("Prepare Production Build Environment") {
//                when {
//                    beforeAgent(true)
//                    allOf {
//                        expression { params.release }
//                        anyOf {
//                            branch 'master'
//                            branch 'main'
//                        }
//                    }
//                }
                steps {
                    switchEnvironment("prod", "${params.awsRegion}")
                }
            }
            stage("[Production] Promote Image") {
                steps {
                    script {
                        withImagePromotionDynamicStage("stage", "prod", "${params.imageTag}")
                    }
                }
            }
            stage("[Production] Deployment") {
//                when {
//                    beforeAgent(true)
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
        }
    }
}
