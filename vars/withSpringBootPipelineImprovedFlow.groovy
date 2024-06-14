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
            IMAGE_BUILD_USERNAME = "${config.ci.cluster_username}"
            IMAGE_BUILD_NAMESPACE = "${config.ci.namespace}"
            IMAGE_BUILD_IGNORE_TLS = "${config.ci.ignoreTls}"
            // Credential used for initial image building and deployment
            SVC_TOKEN = TokenHelper.devTokenName("${config.ci.namespace}", "${params.awsRegion}")
        }

        stages {
            stage("Prepare Dev Build Environment") {
                steps {
                    switchEnvironment("dev", "${params.awsRegion}")
                    setBuildInformation()
                }
            }
            stage("Build & Test App") {
                environment {
                    // Need full path of current workspace for setting path of nvm on $PATH
                    WORKSPACE = pwd()
                }
                steps {
                    gradleBuildOnly(params.release)
                }
            }
            stage("Archive Test Reports") {
                steps {
                    archiveReportAsPdf("Unit", "${env.SERVICE_NAME}/build/reports/tests/test", "index.html", "unit-test-report.pdf", false)
                    archiveReportAsPdf("BDD", "${env.SERVICE_NAME}/build/reports/tests/bddTest", "index.html", "bdd-report.pdf", true)
                    archiveReportAsPdf("Code Coverage", "${env.SERVICE_NAME}/build/reports/jacoco/test/html", "index.html", "coverage-report.pdf", false)
                    //Archive all HTML reports
                    archiveArtifacts artifacts: "${env.SERVICE_NAME}/build/reports/**/*.*"
                }
            }
            stage("Build Image") {
                environment {
                    // Need full path of current workspace for setting path of nvm on $PATH
                    WORKSPACE = pwd()
                }
                steps {
                    gradleBuildImageOnly(params.release, "${env.IMAGE_BUILD_USERNAME}", "${env.IMAGE_BUILD_NAMESPACE}", "${env.IMAGE_BUILD_IGNORE_TLS}")
                }
            }
            stage("[Dev] Deployment") {
                when {
                    expression { params.release }
                    anyOf {
                        triggeredBy 'TimerTrigger'
                        triggeredBy cause: 'UserIdCause'
                    }
                }
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
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
                        steps {
                            scanSysdig("${env.IMAGE_BUILD_NAMESPACE}", "${env.IMAGE_BUILD_USERNAME}")
                        }
                    }

                    stage("Static Analysis (Checkmarx)") {
                        when {
                            expression { env.CHECKMARX_ENABLED.toBoolean() }
                        }
                        steps {
                            scanCheckmarx()
                        }
                    }

                    stage("Dependency Analysis (BlackDuck)") {
                        when {
                            allOf {
                                expression { env.BLACKDUCK_ENABLED.toBoolean() }
                            }
                        }
                        steps {
                            scanBlackduck()
                        }
                    }

                    stage("OWASP Dependency Checker") {
                        when {
                            allOf {
                                expression { env.OWASP_DEPENDENCY_ENABLED.toBoolean() }
                            }
                        }
                        steps {
                            scanOwaspDependency()
                        }
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
                steps {
                    archiveReportsToS3()
                }
            }

            stage("Prepare Staging Build Environment") {
                when {
                    allOf {
                        expression { params.release }
                    }
                }
                steps {
                    switchEnvironment("staging", "${params.awsRegion}")
                }
            }
            stage("[Staging] Deployment") {
                when {
                    allOf {
                        expression { params.release }
                    }
                }
                steps {
                    script {
                        withHelmDeploymentDynamicStage()
                    }
                }
            }
            stage("Performance Testing") {
                when {
                    allOf {
                        expression { params.release }
                        expression { env.PERFORMANCE_TESTING_ENABLED.toBoolean() }
                    }
                }
                steps {
                    withPerformanceTest()
                }
            }
        }
    }
}
