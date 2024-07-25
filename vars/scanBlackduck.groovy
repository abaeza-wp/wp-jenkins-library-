/*
 Used to scan the application binary using Blackduck, for the purpose of dependency vulnerability and license
 management.
 */

def call(Map parameters) {
	def SERVICE_NAME = parameters.serviceName as String
	def BLACKDUCK_API_CREDENTIAL_ID = parameters.blackduckApiCredentialId as String
	def BLACKDUCK_FATJAR = parameters.fatjar as String
	def BLACKDUCK_DETECT_SCRIPT_URL = parameters.detectScriptUrl as String
	def BLACKDUCK_DETECT_SCRIPT = parameters.detectScript as String
	def BLACKDUCK_URL = parameters.blackduckUrl as String
	def BLACKDUCK_PROJECT_NAME = parameters.blackduckProjectName as String
	def SLACK_BLACKDUCK_CHANNEL = parameters.slackChannel as String


	def scanDir = 'blackduckscan'

	withCredentials([
		string(credentialsId: "${BLACKDUCK_API_CREDENTIAL_ID}", variable: 'BLACKDUCK_TOKEN')
	]) {
		// JENKINS_NODE_COOKIES=dontKillMe nohup bash ... & is all used to get detect.sh to run in the background.
		// See more here: https://stackoverflow.com/a/37161006/901641
		sh """
                rm -rf ${scanDir}
                mkdir ${scanDir}
                cp ${BLACKDUCK_FATJAR} ${scanDir}
                curl -O ${BLACKDUCK_DETECT_SCRIPT_URL}
                chmod +x ${BLACKDUCK_DETECT_SCRIPT}
                JENKINS_NODE_COOKIE=dontKillMe nohup bash \\
                ./${BLACKDUCK_DETECT_SCRIPT} --blackduck.url='${BLACKDUCK_URL}' \\
                    --blackduck.api.token='\$BLACKDUCK_TOKEN' --blackduck.trust.cert=true \\
                    --detect.project.name='${BLACKDUCK_PROJECT_NAME}' \\
                    --detect.project.version.name='${env.BUILD_APP_VERSION}' \\
                    --detect.policy.check.fail.on.severities=ALL \\
                    --detect.source.path='${scanDir}' \\
                    --detect.timeout=900 \\
                    --detect.gradle.excluded.projects=performance-testing \\
                    --detect.gradle.excluded.configurations=bdd*,jacoco*,test*,sonarLint*,spotless* \\
                    --detect.risk.report.pdf=true \\
                    --detect.risk.report.pdf.path='${scanDir}' \\
                    --detect.cleanup=false >blackDuckScan.log 2>&1 &
            """
	}

	checkBlackDuckResults(SERVICE_NAME, SLACK_BLACKDUCK_CHANNEL)
}

def checkBlackDuckResults(String serviceName, slackChannel) {
	def scanDir = 'blackduckscan'
	def hasBlackduckScanPassed = false
	// Check the output file for the scan result for up to 15 minutes (180 * 5 seconds).

	try {
		for (int i = 0; i < 180; i++) {
			def blackduckOutput = readFile(file: 'blackDuckScan.log')
			if (!blackduckOutput.contains('Result code of')) {
				sh 'sleep 5'
				continue
			}

			// Dump output from Blackduck
			echo 'Blackduck output:'
			echo blackduckOutput

			// Archive the PDF
			sh """
                mv ${scanDir}/*.pdf ${serviceName}-blackduck-report.pdf
            """

			// Archive artifacts
			archiveArtifacts artifacts: "${serviceName}-blackduck-report.pdf"

			hasBlackduckScanPassed = blackduckOutput.contains('Overall Status: SUCCESS')

			// Mark the build based on the result
			if (hasBlackduckScanPassed) {
				echo 'Black Duck scan appears to be good!'

				return
			} else {
				def status = blackduckOutput.substring(blackduckOutput.indexOf('Overall Status:'))
				status = status.substring(0, status.indexOf('-'))

				error "Black Duck scan failed with ${status}. Check the logs and/or the report!"
			}
		}
	}
	catch (err) {
		echo readFile(file: 'blackDuckScan.log')
		echo "Caught: ${err}"

		error 'Black Duck scan seems to be stuck. Check the logs.'
	}
	finally {
		sendSlackNotificationBlackduck(slackChannel, serviceName, hasBlackduckScanPassed)
	}
}

/*
 Used to send slack notifications with the outcome of blackduck scan
 */

def sendSlackNotificationBlackduck(String slackChannel, String serviceName, Boolean outcome) {
	def message = [
		channel : "${slackChannel}",
		username: 'Sessions Config Blackduck Scan',
		emoji   : '',
		color   : '',
		header  : '',
		text    : '',
		location: "Report can be retrieved from here:\n${env.BUILD_URL}artifact/${serviceName}-blackduck-report.pdf"
	]

	def scanMessage = "Blackduck Scan for *${serviceName}* build *No.${env.BUILD_NUMBER}* for *${env.BUILD_APP_VERSION}* : *${env.BUILD_COMMIT_HASH}*"
	if (outcome) {
		message.emoji = ':duck:'
		message.color = '#31AD72'
		message.header = 'Success'
		message.text = "${scanMessage} has completed successfully.\n"
	} else {
		message.emoji = ':duck:'
		message.color = '#EA5E1A'
		message.header = 'Failed'
		message.text = "${scanMessage} has failed.\n"
	}

	sendSlackNotification(message)
}

/*
 Used to send a slack notification
 */

def sendSlackNotification(String slackCredentialsId, Object message) {
	def payload = '{' +
			"\"channel\":\"${message.channel}\"," +
			"\"username\":\"${message.username}\"," +
			"\"icon_emoji\":\"${message.emoji}\"," +
			'\"attachments\":[' +
			'{' +
			"\"color\":\"${message.color}\"," +
			'\"blocks\":[' +
			'{' +
			'\"type\":\"header\",' +
			'\"text\":{' +
			'\"type\":\"plain_text\",' +
			"\"text\":\"${message.header}\"" +
			'}' +
			'},' +
			'{' +
			'\"type\":\"context\",' +
			'\"elements\":[' +
			'{' +
			'\"type\":\"mrkdwn\",' +
			"\"text\":\"${message.text}\"" +
			'},' +
			'{' +
			'\"type\":\"mrkdwn\",' +
			"\"text\":\"${message.location}\"" +
			'}' +
			']' +
			'}' +
			']' +
			'}' +
			']' +
			'}'

	withCredentials([
		string(credentialsId: "${slackCredentialsId}", variable: 'SLACK_URL')
	]) {
		echo 'Sending notification to slack channel'

		def response = sh(script: "curl -X POST --data-urlencode \"payload=\"'${payload}' '${env.SLACK_URL}'",
		returnStdout: true)

		if (response.contains('ok')) {
			echo 'Slack notification sent'
		} else {
			echo 'Unable to send slack notification'
			echo "Response: ${response}"
		}
	}
}
