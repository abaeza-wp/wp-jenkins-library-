/*
 Used to scan the application source-code using Checkmarx, for the purpose of static analysis.
 */

def call(Map parameters) {
	def SERVICE_NAME = parameters.serviceName as String
	def CHECKMARX_API_CREDENTIAL_ID = parameters.checkmarxCredentialsId as String
	def CHECKMARX_TEAM_PATH = parameters.teamPath as String

	def projectName = env.BRANCH_NAME == 'master' ? "${SERVICE_NAME}_prod" : "${SERVICE_NAME}_dev"

	def excludedFolders = [
		'!*.class',
		'!*.jar',
		'!*.sql',
		'**/.gradle',
		'.gradle',
		'!*.gradle',
		'**/build',
		'**/target',
		'**/src/test',
		'.git',
		'deployment',
		'docs',
		'openapi',
		'**/node_modules',
		'**/nvd',
		'**/blackduckscan',
		'**/native-libs',
		'performance-testing'
	].join(",")
	// Run Checkmarx scanner using Jenkins plugin
	step([$class                       : 'CxScanBuilder',
		comment                      : "version: ${env.BUILD_APP_VERSION}",
		credentialsId                : "${CHECKMARX_API_CREDENTIAL_ID}",
		excludeFolders               : excludedFolders,
		generatePdfReport            : true,
		vulnerabilityThresholdEnabled: true,
		highThreshold                : '0',
		mediumThreshold              : '0',
		lowThreshold                 : '0',
		projectName                  : projectName,
		serverUrl                    : 'https://worldpay.checkmarx.net',
		sourceEncoding               : '1',
		useOwnServerCredentials      : true,
		teamPath                     : "${CHECKMARX_TEAM_PATH}",
		incremental                  : false,
		preset                       : '100003',
		filterPattern                : '!*.class, !*.jar, !*.war',
		waitForResultsEnabled        : true
	])

	// Dump the report
	reportCheckmarx(SERVICE_NAME)
}

def reportCheckmarx(String SERVICE_NAME) {
	// Archive the report
	script {
		sh """
            mv Checkmarx/Reports/CxSASTReport*.pdf ${SERVICE_NAME}-checkmarx-report.pdf
        """

		// Archive artifacts
		archiveArtifacts artifacts: "${SERVICE_NAME}-checkmarx-report.pdf"
	}
}
