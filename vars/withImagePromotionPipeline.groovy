import com.worldpay.context.BuildContext

def getAwsRegions() {
    return [
    "eu-west-1",
    "us-east-1",
    ]
}

def call() {
    pipeline {
        agent
        {
            kubernetes
            {
                label "cd-agent"
                defaultContainer "cd-agent"
//TODO: USing hydra since cd-agent is not yet available
//                label 'hydra'
//                defaultContainer 'hydra'
//                yaml """
//                spec:
//                  containers:
//                  - name: hydra
//                    image: artifactory.luigi.worldpay.io/docker/jenkins-agents/hydra:latest
//                    imagePullPolicy: Always
//                    command:
//                    - cat
//                    tty: true
//                    resources:
//                      limits:
//                        memory: 8Gi
//                        cpu: 4
//                      requests:
//                        memory: 8Gi
//                        cpu: 2
//                  - name: jnlp
//                    resources:
//                      limits:
//                        memory: 500Mi
//                        cpu: 0.5
//                      requests:
//                        memory: 250Mi
//                        cpu: 0.25
//                 """
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
            // The name of the service
            SERVICE_NAME = "${BuildContext.componentName}"

            //If an image tag was provided use that one
            BUILD_APP_VERSION = "${params.imageTag}"
        }

        stages {
            stage("[stage] Prepare Build Environment") {
                steps {
                    switchEnvironment("stage", "${params.awsRegion}")
                    setBuildInformation()
                }
            }
            stage("[stage] Promote Image") {
                steps {
                    script {
                        withImagePromotionDynamicStage("stage", "stage")
                    }
                }
            }
            stage("[stage] Deployment") {
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
                    }
                }
            }
            stage("[prod] Prepare Build Environment") {
                steps {
                    switchEnvironment("prod", "${params.awsRegion}")
                }
            }
            stage("[prod] Promote Image") {
                steps {
                    script {
                        withImagePromotionDynamicStage("stage", "prod")
                    }
                }
            }
            stage("[prod] Deployment") {
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
                    }
                }
            }
        }
    }
}
