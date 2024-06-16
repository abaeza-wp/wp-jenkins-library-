import com.worldpay.context.BuildContext

/**
 * Sets env vars regarding the build that can be used through out the pipeline
 */
def call() {
    warnAboutExperimentalPipelines()

    env.APP_VERSION = getAppVersion()
    env.BUILD_APP_VERSION = (env.BRANCH_NAME.startsWith("PR-") ? env.BRANCH_NAME : env.APP_VERSION)
    env.BUILD_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    env.GIT_COMMIT_TIMESTAMP = sh(script: 'git show -s --format=%cI HEAD', returnStdout: true).trim()

    env.IS_PR_BUILD = env.BRANCH_NAME.startsWith("PR-")

    echo """
            Build Information:

            BRANCH_NAME: ${env.BRANCH_NAME}
            APP_VERSION: ${env.APP_VERSION}
            BUILD_APP_VERSION: ${env.BUILD_APP_VERSION}
            BUILD_COMMIT_HASH: ${env.BUILD_COMMIT_HASH}
            GIT_COMMIT_TIMESTAMP: ${env.GIT_COMMIT_TIMESTAMP}
            
            IS_PR_BUILD = ${env.IS_PR_BUILD}
        """

    //Save Image tag in context
    BuildContext.imageTag = "${env.BUILD_APP_VERSION}"
    // Set job title
    currentBuild.displayName = "#${currentBuild.number} : ${BuildContext.currentBuildProfile.cluster.environment}-${BuildContext.currentBuildProfile.cluster.awsRegion} : ${env.BUILD_APP_VERSION} : ${env.BUILD_COMMIT_HASH}"
}
