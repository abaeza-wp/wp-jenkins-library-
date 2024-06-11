/*
 Used to run gradle build only.
 */

def call() {
	sh """
            ./gradlew ${env.SERVICE_NAME}:clean ${env.SERVICE_NAME}:build ${profiles}
        """
}
