import com.worldpay.context.BuildContext

/**
 *
 * Runs the block of code if the current configuration defines functional environments
 * Usage:
 * withFunctionalEnvironments {
 *   ...
 * }
 */

def call(block) {
    if (BuildContext.useFunctionalEnvironments) {
        return block.call()
    }
}
