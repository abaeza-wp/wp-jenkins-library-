/*
 Used to send slack notifications with the outcome of blackduck scan
 */

def call(Boolean outcome) {

    def message = [
        channel : "$env.SLACK_BLACKDUCK_CHANNEL",
        username: "Sessions Config Blackduck Scan",
        emoji   : "",
        color   : "",
        header  : "",
        text    : "",
        location: "Report can be retrieved from here:\n${env.BUILD_URL}artifact/${env.SERVICE_NAME}-blackduck-report.pdf"
    ]

    def scanMessage = "Blackduck Scan for *${env.SERVICE_NAME}* build *No.${env.BUILD_NUMBER}* for *${env.BUILD_APP_VERSION}* : *${env.BUILD_COMMIT_HASH}*"
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
