import com.worldpay.context.BuildContext
import com.worldpay.utils.TokenHelper

def getAwsRegions() {
    return [
    "eu-west-1",
    "us-east-1",
    ]
}

def call() {
    pipeline {
        agent {
            kubernetes {
                label 'hydra'
                defaultContainer 'hydra'
                yaml """
                spec:
                  containers:
                  - name: hydra
                    image: artifactory.luigi.worldpay.io/docker/jenkins-agents/hydra:latest
                    imagePullPolicy: Always
                    command:
                    - cat
                    tty: true
                    resources:
                      limits:
                        memory: 8Gi
                        cpu: 4
                      requests:
                        memory: 8Gi
                        cpu: 2
                  - name: jnlp
                    resources:
                      limits:
                        memory: 500Mi
                        cpu: 0.5
                      requests:
                        memory: 250Mi
                        cpu: 0.25
                 """
            }
        }
        parameters {
            choice(
            name: "awsRegion",
            choices: getAwsRegions(),
            description: "The target deployment aws region."
            )
            booleanParam(
            name: "release",
            defaultValue: true,
            description: "Runs additional scans for release deployments, not needed for development"
            )
        }

        environment {
            // Read Jenkins configuration
            config = readYaml(file: "deployment/jenkins.yaml")

            // The name of the service
            SERVICE_NAME = "${BuildContext.componentName}"

            // Checkmarx
            CHECKMARX_ENABLED = "${config.checkmarx.enabled}"
            CHECKMARX_TEAM_PATH = "${config.checkmarx.teamPath}"
            CHECKMARX_API_CREDENTIAL_ID = "${config.checkmarx.api.credentialId}"

            // Blackduck
            BLACKDUCK_ENABLED = "${config.blackduck.enabled}"
            BLACKDUCK_PROJECT_NAME = "${config.blackduck.projectName}"
            BLACKDUCK_URL = "https://fis2.app.blackduck.com"
            BLACKDUCK_DETECT_SCRIPT_URL = "https://detect.synopsys.com/detect8.sh"
            BLACKDUCK_DETECT_SCRIPT = "detect8.sh"
            BLACKDUCK_FATJAR = "${env.SERVICE_NAME}/build/libs/${env.SERVICE_NAME}-0.0-SNAPSHOT.jar"
            BLACKDUCK_API_CREDENTIAL_ID = "${config.blackduck.api.credentialId}"

            // OWASP Dependency Checker
            OWASP_DEPENDENCY_ENABLED = "${config.owaspDependencyChecker.enabled}"
            OWASP_DEPENDENCY_NVD_BUCKET_NAME = "${config.owaspDependencyChecker.awsBucket.name}"
            OWASP_DEPENDENCY_NVD_BUCKET_CREDENTIAL_ID = "${config.owaspDependencyChecker.awsBucket.credentialId}"

            // Sysdig Image Scanning
            SYSDIG_IMAGE_SCANNING_ENABLED = "${config.sysdigImageScanning.enabled}"
            SYSDIG_IMAGE_SCANNING_API_CREDENTIAL_ID = "${config.sysdigImageScanning.api.credentialId}"

            // Performance testing
            PERFORMANCE_TESTING_ENABLED = "${config.performanceTesting.enabled}"
            PERFORMANCE_TESTING_WAIT_SECONDS = "${config.performanceTesting.initialWaitSeconds}"

            // Report Archiving
            REPORT_ARCHIVING_ENABLED = "${config.reportArchiving.enabled}"
            REPORT_ARCHIVING_BUCKET_NAME = "${config.reportArchiving.awsBucket.name}"
            REPORT_ARCHIVING_BUCKET_CREDENTIAL_ID = "${config.reportArchiving.awsBucket.credentialId}"

            // Slack notifications
            SLACK_WEBHOOK_URL = "${config.slack.webhookUrl}"
            SLACK_BLACKDUCK_CHANNEL = "$config.slack.channels.blackduck"
            SLACK_SYSDIG_CHANNEL = "$config.slack.channels.sysdig"

            //Image Build (dev)
            DEV_CLUSTER_USERNAME = "${config.ci.cluster_username}"
            IMAGE_BUILD_NAMESPACE = "${config.ci.namespace}"
            IMAGE_BUILD_IGNORE_TLS = "${config.ci.ignore_tls}"
            // Credential used for initial image building and deployment
            SVC_TOKEN = TokenHelper.devTokenName("${config.ci.namespace}", "${params.awsRegion}")
        }

        stages {
            stage("Prepare Dev Build Environment") {
                steps {
                    script {


                        stage("Build & Test App") {
                            echo "gradle build"
                        }
                        stage("Archive Test Reports") {
                            echo "archive test"
                        }
                        stage("Build Image") {
                            echo "build image"
                        }
                        stage("[Dev] Deployment") {
                            when {
                                expression { params.release }
                                anyOf {
                                    triggeredBy 'TimerTrigger'
                                    triggeredBy cause: 'UserIdCause'
                                }
                            }
                            script {
                                echo "helm deployment"
                            }
                        }
                    }

                    stage("Security Testing") {
                        parallel {
                            stage("Image Scan (Sysdig)") {
                                when {
                                    allOf {
                                        expression { env.SYSDIG_IMAGE_SCANNING_ENABLED.toBoolean() }
                                    }
                                }
                                echo "sysdig"
                            }

                            stage("Static Analysis (Checkmarx)") {
                                when {
                                    expression { env.CHECKMARX_ENABLED.toBoolean() }
                                }
                                echo "scanCheckmarx"
                            }

                            stage("Dependency Analysis (BlackDuck)") {
                                when {
                                    allOf {
                                        expression { env.BLACKDUCK_ENABLED.toBoolean() }
                                    }
                                }
                                echo "scanBlackduck"
                            }

                            stage("OWASP Dependency Checker") {
                                when {
                                    allOf {
                                        expression { env.OWASP_DEPENDENCY_ENABLED.toBoolean() }
                                    }
                                }
                                echo "scanOwaspDependency"
                            }
                        }
                    }
                    stage("Archive reports in S3") {
                        when {
                            allOf {
                                expression { env.REPORT_ARCHIVING_ENABLED.toBoolean() }
                                expression { params.release }
                                anyOf {
                                    branch 'master'
                                    branch 'main'
                                }
                            }
                        }
                        echo "archiveReportsToS3"
                    }

                    stage("Prepare Staging Build Environment") {
                        when {
                            allOf {
                                expression { params.release }
                                anyOf {
                                    branch 'master'
                                    branch 'main'
                                }
                            }
                        }
                        echo "switchStaging"
                    }
                    stage("[Staging] Deployment") {
                        when {
                            allOf {
                                expression { params.release }
                                anyOf {
                                    branch 'master'
                                    branch 'main'
                                }
                            }
                        }
                        script {
                            echo "withHelmDeploymentDynamicStage"
                        }
                    }
                    stage("Performance Testing") {
                        when {
                            allOf {
                                expression { params.release }
                                expression { env.PERFORMANCE_TESTING_ENABLED.toBoolean() }
                                anyOf {
                                    branch 'master'
                                    branch 'main'
                                }
                            }
                        }
                        echo "withPerformanceTest"
                    }

                }
            }
        }
    }
}
