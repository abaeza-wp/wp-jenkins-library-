import com.worldpay.pipeline.BuildContext

/**
 *
 * Runs the block of code if the current configuration defines functional environments
 * Usage:
 * withFunctionalEnvironments {
 *   ...
 * }
 */

def call(block) {
    if (BuildContext.currentBuildProfile.useFunctionalEnvironments) {
        return block.call()
    }
}
