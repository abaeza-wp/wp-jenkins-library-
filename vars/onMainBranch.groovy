/**
 *
 * Runs the block of code if the current branch is either Master or Main
 * Usage:
 * onMainBranch {
 *   ...
 * }
 */

def call(Closure body) {
    if (env.BRANCH_NAME === "master" || env.BRANCH_NAME === "main") {
        body.call()
    }
}
