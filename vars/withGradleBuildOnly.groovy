/*
    Used to build the container image.

    Scroll further down to the executeImageBuild function to customise the built steps.
*/

def call(Closure body) {
    script
    {
        def profiles = ""

        if (params.release) {
            profiles += "-Prelease"
        }

        sh """
            ./gradlew ${env.SERVICE_NAME}:clean ${env.SERVICE_NAME}:build ${profiles}
        """
    }
    body.call()
}
