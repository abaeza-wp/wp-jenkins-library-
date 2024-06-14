import com.worldpay.pipeline.BuildContext

def call() {
    call(null)
}

def call(String profileName) {
    warnAboutExperimentalPipelines()

    env.APP_VERSION = getVersion()
    env.BUILD_APP_VERSION = (env.BRANCH_NAME.startsWith("PR-") ? env.BRANCH_NAME : env.APP_VERSION)
    env.BUILD_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    env.GIT_REF = (env.BRANCH_NAME ? env.BRANCH_NAME : params.gitReference)
    env.GIT_COMMIT_TIMESTAMP = sh(script: 'git show -s --format=%cI HEAD', returnStdout: true).trim()

    env.IS_PR_BUILD = env.BRANCH_NAME.startsWith("PR-")

    echo """
            Build Information:

            BRANCH_NAME: ${env.BRANCH_NAME}
            BUILD_APP_VERSION: ${env.BUILD_APP_VERSION}
            BUILD_COMMIT_HASH: ${env.BUILD_COMMIT_HASH}
            GIT_REF: ${env.GIT_REF}
            
            IS_PR_BUILD = ${env.IS_PR_BUILD}
        """

    //Save Image tag in context
    BuildContext.setImageTag("${env.BUILD_APP_VERSION}")
    // Set job title
    currentBuild.displayName = "#${currentBuild.number} : ${profileName} : ${env.BUILD_APP_VERSION} : ${env.BUILD_COMMIT_HASH}"
}
