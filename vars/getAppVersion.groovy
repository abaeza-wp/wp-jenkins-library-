/*
 Used to determine the version of the application.
 This will use the git tag.
 If the immediate commit is not a tag, it's a combination of the last tag and short-hash.
 You can change the logic here, but not recommended. Your version should be compatible with the allowed characters
 for container tag names (e.g. you cannot have dots etc). See:
 https://docs.docker.com/engine/reference/commandline/tag/#description
 */

def call() {
    // Ensure we use hyphens rather than dots, as this is used for image names etc
    def app_version = sh(script: 'git describe --always --tags --abbrev=5', returnStdout: true).trim().replaceAll('\\.', '-')

    // Validate the version is present, otherwise immediately fail the job
    if (app_version == null || app_version.length() == 0) {
        error('Unable to determine application version')
    }

    echo "Application version: ${app_version}"
    return app_version
}
