
def call() {

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
                    load("deployment/boilerplate/scripts/pipeline/checkmarx.groovy").runCheckmarx()
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

    body.call()
}

