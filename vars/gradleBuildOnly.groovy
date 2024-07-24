/*
 Used to run gradle build only.
 */

def call(Boolean isRelease) {
    def profiles = ''

    if (isRelease) {
        profiles += '-Prelease'
    }

    sh """
            ./gradlew ${env.SERVICE_NAME}:clean ${env.SERVICE_NAME}:build ${profiles}
    """
}
