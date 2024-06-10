package com.worldpay

class SpringBootPipeline implements Serializable {
    def tenant
    def component

    SpringBootPipeline(tenant, component) {
        this.tenant = tenant
        this.component = component
    }

    def call() {

        pipeline
        {
            agent
            {
                kubernetes
                {
                    label "hpp"
                    defaultContainer "hpp"
                    yaml libraryResource("agents/k8s/hpp.yaml")
                }
            }

            triggers
            {
                // it automatically runs every Monday at around midnight
                cron("H 0 * * 1")
            }

            environment
            {
                // Read Jenkins configuration
                config = readYaml (file: "deployment/jenkins.yaml")

                // The name of the service
                SERVICE_NAME                                = "${config.service.name}"

                // Checkmarx
                CHECKMARX_ENABLED                           = "${config.checkmarx.enabled}"
                CHECKMARX_TEAM_PATH                         = "${config.checkmarx.teamPath}"
                CHECKMARX_API_CREDENTIAL_ID                 = "${config.checkmarx.api.credentialId}"

                // Blackduck
                BLACKDUCK_ENABLED                           = "${config.blackduck.enabled}"
                BLACKDUCK_PROJECT_NAME                      = "${config.blackduck.projectName}"
                BLACKDUCK_URL                               = "https://fis2.app.blackduck.com"
                BLACKDUCK_DETECT_SCRIPT_URL                 = "https://detect.synopsys.com/detect8.sh"
                BLACKDUCK_DETECT_SCRIPT                     = "detect8.sh"
                BLACKDUCK_FATJAR                            = "${env.SERVICE_NAME}/build/libs/${env.SERVICE_NAME}-0.0-SNAPSHOT.jar"
                BLACKDUCK_API_CREDENTIAL_ID                 = "${config.blackduck.api.credentialId}"

                // OWASP Dependency Checker
                OWASP_DEPENDENCY_ENABLED                    = "${config.owaspDependencyChecker.enabled}"
                OWASP_DEPENDENCY_NVD_BUCKET_NAME            = "${config.owaspDependencyChecker.awsBucket.name}"
                OWASP_DEPENDENCY_NVD_BUCKET_CREDENTIAL_ID   = "${config.owaspDependencyChecker.awsBucket.credentialId}"

                // Sysdig Image Scanning
                SYSDIG_IMAGE_SCANNING_ENABLED               = "${config.sysdigImageScanning.enabled}"
                SYSDIG_IMAGE_SCANNING_API_CREDENTIAL_ID     = "${config.sysdigImageScanning.api.credentialId}"

                // Performance testing
                PERFORMANCE_TESTING_ENABLED                 = "${config.performanceTesting.enabled}"
                PERFORMANCE_TESTING_WAIT_SECONDS            = "${config.performanceTesting.initialWaitSeconds}"

                // Report Archiving
                REPORT_ARCHIVING_ENABLED                    = "${config.reportArchiving.enabled}"
                REPORT_ARCHIVING_BUCKET_NAME                = "${config.reportArchiving.awsBucket.name}"
                REPORT_ARCHIVING_BUCKET_CREDENTIAL_ID       = "${config.reportArchiving.awsBucket.credentialId}"

                // Credential used for deployments
                def profileConfig = readYaml(file: "deployment/profiles/${profile}.yml")
                SVC_TOKEN = tokenNameOf(profileConfig.deploy.namespace, profile)

                // Slack notifications
                SLACK_WEBHOOK_URL = "${config.slack.webhookUrl}"
                SLACK_BLACKDUCK_CHANNEL = "$config.slack.channels.blackduck"
                SLACK_SYSDIG_CHANNEL = "$config.slack.channels.sysdig"
            }

            parameters
            {
                choice(
                name: "profile",
                choices: getProfiles(),
                description: "The target deployment profile."
                )

                string(
                name: "gitReference",
                defaultValue: "master",
                description: "Branch or tag that we want to deploy"
                )

                booleanParam(
                name: "release",
                defaultValue: true,
                description: "Runs additional scans for release deployments, not needed for development"
                )
            }

            stages
            {
                stage("Build Image")
                {
                    environment
                    {
                        // Need full path of current workspace for setting path of nvm on $PATH
                        WORKSPACE = pwd()
                    }
                    steps
                    {
                        script
                        {
                            load("deployment/boilerplate/scripts/build-image.groovy").buildImage()
                        }
                    }
                }

                stage("Testing")
                {
                    parallel
                    {
                        stage("Performance")
                        {
                            when
                            {
                                allOf
                                {
                                    expression { params.release }
                                    expression { params.profile.contains("staging") }
                                    expression { env.PERFORMANCE_TESTING_ENABLED.toBoolean() }
                                }
                            }
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/performance-test.groovy").performanceTest()
                                }
                            }
                        }

                        stage("Image Scan (Sysdig)")
                        {
                            when
                            {
                                allOf
                                {
                                    expression { env.SYSDIG_IMAGE_SCANNING_ENABLED.toBoolean() }
                                    expression { params.release }
                                }
                            }
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/sysdig-image-scan.groovy").sysdigImageScan()
                                }
                            }
                        }

                        stage("Static Analysis (Checkmarx)")
                        {
                            when
                            {
                                expression { env.CHECKMARX_ENABLED.toBoolean() }
                                expression { params.release }
                            }
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/checkmarx.groovy").runCheckmarx(params.gitReference)
                                }
                            }
                        }

                        stage("Dependency Analysis (BlackDuck)")
                        {
                            when
                            {
                                allOf
                                {
                                    expression { env.BLACKDUCK_ENABLED.toBoolean() }
                                    expression { params.release }
                                }
                            }
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/blackduck.groovy").runBlackduck()
                                }
                            }
                        }

                        stage("Code Coverage Report")
                        {
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/reporting.groovy").reportCodeCoverage()
                                }
                            }
                        }

                        stage("Unit Tests Report")
                        {
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/reporting.groovy").reportUnit()
                                }
                            }
                        }

                        stage("OWASP Dependency Checker")
                        {
                            when
                            {
                                allOf
                                {
                                    expression { env.OWASP_DEPENDENCY_ENABLED.toBoolean() }
                                }
                            }
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/owasp-dependency-checker.groovy").owaspDependencyChecker()
                                }
                            }
                        }

                        stage("BDD Report")
                        {
                            steps
                            {
                                script
                                {
                                    load("deployment/boilerplate/scripts/pipeline/reporting.groovy").reportBDD()
                                }
                            }
                        }
                    }
                }

                stage("Archive Reports")
                {
                    when
                    {
                        allOf
                        {
                            expression { env.REPORT_ARCHIVING_ENABLED.toBoolean() }
                            expression { params.release }
                            expression { params.profile.contains("staging") }
                        }
                    }
                    steps
                    {
                        script
                        {
                            load("deployment/boilerplate/scripts/pipeline/reporting.groovy").archiveReports()
                        }
                    }
                }

            }
        }

        stage("Deploy")
        {
            when
            {
                expression
                {
                    gitReference = (env.BRANCH_NAME ?  env.BRANCH_NAME : params.gitReference)
                    return !gitReference.startsWith('PR')
                }
            }
            steps
            {
                script
                {
                    load("deployment/boilerplate/scripts/deploy.groovy").deploy(params.profile)
                }
            }
        }

    }

}
