/*
 Used to run gradle build only.
 */

def call() {

    def profiles = ""

    if (params.release) {
        profiles += "-Prelease"
    }

    sh """
            ./gradlew ${env.SERVICE_NAME}:clean ${env.SERVICE_NAME}:build ${profiles}
    """
}
