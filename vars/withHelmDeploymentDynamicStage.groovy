import com.worldpay.pipeline.BuildConfigurationContext

/**
 *
 * Runs the block of code if the current configuration defines functional environments
 * Usage:
 * withFunctionalEnvironments {
 *   ...
 * }
 */

def call(environmentName) {
	if (BuildConfigurationContext.shouldUseFunctionalEnvironments()) {
		for (fEnv in BuildConfigurationContext.getFunctionalEnvironments()) {
			stage("[${environmentName}][${fEnv}] Deploy Application") {
				environment {
					DEPLOYMENT_FUNCTIONAL_ENVIRONMENT = "$fEnv"
					SVC_TOKEN = "svc_token-${env.FULL_APP_NAME}-${fEnv}-${params.profile}"
				}
				steps {
					helmDeployment()
				}
			}
		}
	} else {
		stage("[${environmentName}] Deploy Application") {
			environment {
				SVC_TOKEN = "svc_token-${env.FULL_APP_NAME}-${params.profile}"
			}
			steps {
				helmDeployment()
			}
		}
	}
}
