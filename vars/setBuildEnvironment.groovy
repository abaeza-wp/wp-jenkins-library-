def call(Closure body) {
    script
    {
        env.BUILD_APP_VERSION = load("deployment/boilerplate/scripts/get-version.groovy").getVersion()
        env.BUILD_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
        env.BUILD_TAG_OR_BRANCH = sh(script: "git describe --contains --all HEAD", returnStdout: true).replaceAll('/', '-').trim()
        env.GIT_REF = (env.BRANCH_NAME ? env.BRANCH_NAME : params.gitReference)
        echo "==========================================================================================="
        echo "Set pipeline environment variables"
        echo "BRANCH_NAME: ${env.BRANCH_NAME}"
        echo "BUILD_APP_VERSION: ${env.BUILD_APP_VERSION}"
        echo "BUILD_COMMIT_HASH: ${env.BUILD_COMMIT_HASH}"
        echo "BUILD_TAG_OR_BRANCH: ${env.BUILD_TAG_OR_BRANCH}"
        echo "GIT_REF: ${env.GIT_REF}"
        echo "==========================================================================================="
    }
    body.call()
}

