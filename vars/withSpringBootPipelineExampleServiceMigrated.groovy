import com.worldpay.context.BuildContext

def getProfiles() {
    return [
        "dev-euwest1",
        "staging-euwest1",
        "staging-useast1",
    ]
}

def tokenNameOf(namespace, profileName) {
    def tokenSuffix = profileName.replace('-live', '')
            .replace('-try', '')

    return "svc_token-${namespace}-${tokenSuffix}"
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
                    name: "profile",
                    choices: getProfiles(),
                    description: "The target deployment profile."
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

            // Credential used for deployments
            def profileConfig = readYaml(file: "deployment/profiles/${profile}.yml")
            SVC_TOKEN = tokenNameOf(profileConfig.deploy.namespace, profile)

            // Slack notifications
            SLACK_WEBHOOK_URL = "${config.slack.webhookUrl}"
            SLACK_BLACKDUCK_CHANNEL = "$config.slack.channels.blackduck"
            SLACK_SYSDIG_CHANNEL = "$config.slack.channels.sysdig"

            //Add AWS region from profile name to make compatible
            AWS_REGION = BuildContext.mapAwsRegionFromProfile("${params.profile}")

            //Image Build (dev)
            DEV_CLUSTER_USERNAME = "${profileConfig.deploy.cluster_username}"
            IMAGE_BUILD_NAMESPACE = "${profileConfig.deploy.namespace}"
            IMAGE_BUILD_IGNORE_TLS = "${profileConfig.deploy.ignore_tls}"

            WORKSPACE = pwd()
        }

        stages {
            stage("Set Build Information") {
                steps {
                    switchEnvironment("dev", "${env.AWS_REGION}")
                    setBuildInformation()
                }
            }
            stage("Build Image") {
                steps {
                    gradleBuildImage()
                }
            }

            stage("Deployment") {
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

            stage("Performance") {
                when {
                    allOf {
                        expression { params.release }
                        expression { params.profile.contains("staging") }
                        expression {
                            env.PERFORMANCE_TESTING_ENABLED.toBoolean()
                        }
                    }
                }
                steps {
                    withPerformanceTest()
                }
            }

            stage("Security Testing") {
                parallel {
                    stage("Image Scan (Sysdig)") {
                        when {
                            allOf {
                                expression {
                                    env.SYSDIG_IMAGE_SCANNING_ENABLED.toBoolean()
                                }
                            }
                        }
                        steps {
                            scanSysdig("${env.IMAGE_BUILD_NAMESPACE}", "${env.DEV_CLUSTER_USERNAME}")
                        }
                    }

                    stage("Static Analysis (Checkmarx)") {
                        when {
                            expression {
                                env.CHECKMARX_ENABLED.toBoolean()
                            }
                        }
                        steps {
                            scanCheckmarx()
                        }
                    }

                    stage("Dependency Analysis (BlackDuck)") {
                        when {
                            allOf {
                                expression {
                                    env.BLACKDUCK_ENABLED.toBoolean()
                                }
                            }
                        }
                        steps {
                            scanBlackduck()
                        }
                    }

                    stage("OWASP Dependency Checker") {
                        when {
                            allOf {
                                expression {
                                    env.OWASP_DEPENDENCY_ENABLED.toBoolean()
                                }
                            }
                        }
                        steps {
                            scanOwaspDependency()
                        }
                    }

                    stage("Code Coverage Report") {
                        steps {
                            archiveReportAsPdf("Code Coverage", "${env.SERVICE_NAME}/build/reports/jacoco/test/html", "index.html", "coverage-report.pdf", false)
                        }
                    }

                    stage("Unit Tests Report") {
                        steps {
                            archiveReportAsPdf("Unit", "${env.SERVICE_NAME}/build/reports/tests/test", "index.html", "unit-test-report.pdf", false)
                        }
                    }

                    stage("BDD Report") {
                        steps {
                            archiveReportAsPdf("BDD", "${env.SERVICE_NAME}/build/reports/tests/bddTest", "index.html", "bdd-report.pdf", true)
                        }
                    }
                }
            }

            stage("Archive reports in S3") {
                when {
                    allOf {
                        expression { env.REPORT_ARCHIVING_ENABLED.toBoolean() }
                        expression { params.release }
                        expression {
                            params.profile.contains("staging")
                        }
                    }
                }
                steps {
                    archiveReportsToS3()
                }
            }

            stage("Archive HTML Reports artifacts") {
                steps {
                    archiveArtifacts artifacts: "${env.SERVICE_NAME}/build/reports/**/*.*"
                }
            }
        }
    }
}
