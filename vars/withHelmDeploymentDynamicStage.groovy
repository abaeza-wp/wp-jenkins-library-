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
			stage("[${environmentName}] Deploy ${fEnv} Functional Environment") {
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
			when {
				expression { params.release }
				anyOf {
					triggeredBy 'TimerTrigger'
					triggeredBy cause: 'UserIdCause'
				}
			}
			environment {
				SVC_TOKEN = "svc_token-${env.FULL_APP_NAME}-live-${params.profile}"
			}
			steps {
				helmDeployment()
			}
		}
	}
}
