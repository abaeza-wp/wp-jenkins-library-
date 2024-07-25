/*
 Used to run gradle build only.
 */

def call(Map parameters) {
    def SERVICE_NAME = parameters.serviceName as String

    sh """
            ./gradlew ${SERVICE_NAME}:clean ${SERVICE_NAME}:build
    """
}
