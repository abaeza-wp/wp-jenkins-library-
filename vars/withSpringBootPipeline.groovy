def getProfiles() {
	return [
		"dev-euwest1",
		"staging-euwest1",
		"staging-useast1",
	]
}
def getAwsRegions() {
	return [
		"eu-west-1",
		"us-east-1",
	]
}
def tokenNameOf(namespace, profileName) {
	def tokenSuffix = profileName.replace('-live', '')
			.replace('-try', '')

	return "svc_token-${namespace}-${tokenSuffix}"
}

def cronExpression() {
	return BRANCH_NAME == "master" ? "H 0 * * 1" : ""
}

def call(arguments) {
	String tenant = arguments.tenant
	String component = arguments.component

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

		triggers {
			// it automatically runs every Monday at around midnight
			cron(cronExpression())
		}

		environment {
			// Read Jenkins configuration
			config = readYaml(file: "deployment/jenkins.yaml")

			// The name of the service
			SERVICE_NAME = "${component}"
			FULL_APP_NAME = "${tenant}-${component}"

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
		}

		parameters {
			choice(
					name: "profile",
					choices: getProfiles(),
					description: "The target deployment profile."
					)
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

		stages {
			stage("Set Build Information") {
				steps {
					setBuildInformation()
					switchEnvironment("dev")
				}
			}
			stage("Build Image") {
				environment {
					// Need full path of current workspace for setting path of nvm on $PATH
					WORKSPACE = pwd()
				}
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
						expression { env.PERFORMANCE_TESTING_ENABLED.toBoolean() }
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
								expression { env.SYSDIG_IMAGE_SCANNING_ENABLED.toBoolean() }
							}
						}
						steps {
							scanSysdig()
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
						expression { params.profile.contains("staging") }
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
