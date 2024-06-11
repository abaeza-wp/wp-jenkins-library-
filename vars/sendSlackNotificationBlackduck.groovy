/*
    Used to send slack notifications with the outcome of blackduck scan
*/

def call(Boolean outcome, Closure body) {
    script
    {
        def message = [
        channel : "$env.SLACK_BLACKDUCK_CHANNEL",
        username: "Sessions Config Blackduck Scan",
        emoji   : "",
        color   : "",
        header  : "",
        text    : "",
        location: "Report can be retrieved from here:\n${env.BUILD_URL}artifact/${env.SERVICE_NAME}-blackduck-report.pdf"
        ]

        var scanMessage = "Blackduck Scan for *${env.SERVICE_NAME}* build *No.${env.BUILD_NUMBER}* for *${env.BUILD_APP_VERSION}* : *${env.BUILD_COMMIT_HASH}*"
        if (outcome) {
            message.emoji = ":duck:"
            message.color = "#31AD72"
            message.header = "Success"
            message.text = "${scanMessage} has completed successfully."
        } else {
            message.emoji = ":duck:"
            message.color = "#EA5E1A"
            message.header = "Failed"
            message.text = "${scanMessage} has failed.\n"
        }

        sendSlackNotification(message)
    }
    body.call()
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
