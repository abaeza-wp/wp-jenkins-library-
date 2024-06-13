import com.worldpay.pipeline.BuildConfigurationContext

/**
 *
 * Runs the block of code if the current configuration defines functional environments
 * Usage:
 * withFunctionalEnvironments {
 *   ...
 * }
 */

def call() {
    call(null)
}

def call(environmentName) {
    def stageName = environmentName != null ? "[${environmentName}] Deploy Application" : "Deploy Application"
    if (BuildConfigurationContext.shouldUseFunctionalEnvironments()) {
        for (fEnv in BuildConfigurationContext.getFunctionalEnvironments()) {
            stage("${stageName} [${fEnv}]") {
                environment {
                    SVC_TOKEN = "svc_token-${env.FULL_APP_NAME}-${fEnv}-${params.profile}"
                }
                helmDeployment("${fEnv}")
            }
        }
    } else {
        stage("${stageName}") {
            environment {
                SVC_TOKEN = "svc_token-${env.FULL_APP_NAME}-${params.profile}"
            }
            helmDeployment()
        }
    }
}
