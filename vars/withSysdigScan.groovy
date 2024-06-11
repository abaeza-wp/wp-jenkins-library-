/*
    Used to scan the built container image using Sysdig Secure, for the purpose of image dependency vulnerability
    management and best practices.
*/

def call(Closure body) {

    script
    {
        echo "Running Sysdig scan..."


        def profile = readYaml(file: "deployment/profiles/${params.profile}.yml")
        def hasSysdigScanPassed = false
        def resultsUrl = ""

        registry = "${profile.build.docker_registry}"
        namespace = "${profile.deploy.namespace}"
        imageName = "${env.SERVICE_NAME}"
        imageTag = "${env.BUILD_APP_VERSION}"

        imageUrl = "${registry}/${namespace}/${imageName}:${imageTag}"
        echo "Image URL: ${imageUrl}"

        try {
            echo "Executing sysdig scan..."
            sh """
                SECURE_API_TOKEN=${SYSDIG_API_KEY} \
                REGISTRY_USER=${profile.deploy.cluster_username} \
                REGISTRY_PASSWORD=${KUBERNETES_TOKEN} \
                \
                sysdig-cli-scanner \
                    --apiurl=https://secure.sysdig.com ${imageUrl} \
                    --loglevel=debug \
                    --console-log \
                    --output-json=./scan-result.json \
                    --dbpath=/tmp/ \
                    --skiptlsverify
            """

            sh """
                mv scan-result.json ${env.SERVICE_NAME}-sysdig-scan-result.json
            """
            archiveArtifacts artifacts: "${env.SERVICE_NAME}-sysdig-scan-result.json"

            echo "Checking status.policy in the sysdig report"
            def scanOutput = readJSON file: "./${env.SERVICE_NAME}-sysdig-scan-result.json"

            hasSysdigScanPassed = scanOutput.policies.status == "accepted" || scanOutput.policies.status == "passed"
            echo "Report URL: ${scanOutput.info.resultURL}"
            resultsUrl = scanOutput.info.resultURL

            if (!hasSysdigScanPassed) {
                unstable("Sysdig scan failed - policy violation")
            }

        }
        catch (err) {
            echo "Caught: ${err}"
            unstable("Sysdig scan failed")
        }
        finally {
            sendSlackNotificationSysdig(hasSysdigScanPassed, resultsUrl)
        }
    }
    body.call()
}

/*
    Used to send slack notifications with the outcome of sysdig scan
*/

def sendSlackNotificationSysdig(Boolean outcome, String reportLocation) {
    def message = [
    channel : "$env.SLACK_SYSDIG_CHANNEL",
    username: "Sessions Config Sysdig Scan",
    emoji   : "",
    color   : "",
    header  : "",
    text    : "",
    location: "Report can be retrieved from here:\n${reportLocation}"
    ]

    def scanMessage = "Sysdig Scan for *${env.SERVICE_NAME}* build *No.${env.BUILD_NUMBER}* for *${env.BUILD_APP_VERSION}* : *${env.BUILD_COMMIT_HASH}*"

    if (outcome) {
        message.emoji = ":+1:"
        message.color = "#31AD72"
        message.header = "Success"
        message.text = "${scanMessage} has completed successfully.\n"
    } else {
        message.emoji = ":-1:"
        message.color = "#EA5E1A"
        message.header = "Failed"
        message.text = "${scanMessage} has failed.\n"
    }

    sendSlackNotification(message)
}


/*
    Used to send a slack notification
*/

def sendSlackNotification(Object message) {

    def payload = "{" +
    "\"channel\":\"${message.channel}\"," +
    "\"username\":\"${message.username}\"," +
    "\"icon_emoji\":\"${message.emoji}\"," +
    "\"attachments\":[" +
    "{" +
    "\"color\":\"${message.color}\"," +
    "\"blocks\":[" +
    "{" +
    "\"type\":\"header\"," +
    "\"text\":{" +
    "\"type\":\"plain_text\"," +
    "\"text\":\"${message.header}\"" +
    "}" +
    "}," +
    "{" +
    "\"type\":\"context\"," +
    "\"elements\":[" +
    "{" +
    "\"type\":\"mrkdwn\"," +
    "\"text\":\"${message.text}\"" +
    "}," +
    "{" +
    "\"type\":\"mrkdwn\"," +
    "\"text\":\"${message.location}\"" +
    "}" +
    "]" +
    "}" +
    "]" +
    "}" +
    "]" +
    "}"

    withCredentials([string(credentialsId: "${env.SLACK_WEBHOOK_URL}", variable: "SLACK_URL")]) {
        echo "Sending notification to slack channel"

        def response = sh(script: "curl -X POST --data-urlencode \"payload=\"'${payload}' '${env.SLACK_URL}'", returnStdout: true)

        if (response.contains("ok")) {
            echo "Slack notification sent"
        } else {
            echo "Unable to send slack notification"
            echo "Response: ${response}"
        }
    }
}

