import com.worldpay.pipeline.BuildConfigurationContext

/**
 *
 * Runs the block of code if the current configuration defines functional environments
 * Usage:
 * withFunctionalEnvironments {
 *   ...
 * }
 */

def call(block) {
    if (BuildConfigurationContext.currentBuildConfig.useFunctionalEnvironments) {
        return block.call()
    }
}
