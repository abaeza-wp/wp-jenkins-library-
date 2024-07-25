/*
 Used to setup the namespace on the Kubernetes dev cluster, where namespaces can be created dynamically without
 the consumers repository.
 You can add custom logic to the end of the function, but not recommended.
 */

def call(profile) {
	echo 'Creating namespace...'

	// Create the namespace (allowed to fail)
	def labels = ''
	if (profile.deploy.create_namespace.buc_code != null) {
		labels += "--description=\"${profile.deploy.create_namespace.buc_code}\""
	}

	sh "oc new-project ${profile.deploy.namespace} --display-name=${profile.deploy.namespace} ${labels} || true"

	// Set labels for Splunk logging
	sh "oc label namespace ${profile.deploy.namespace} splunk.logging.fis.dev/index=${profile.logging.splunk.index} splunk.logging.fis.dev/token=${profile.logging.splunk.token} --overwrite=true || true"

	// Set the client to the new namespace (best practice in case of any resources without a namespace)
	sh "oc project ${profile.deploy.namespace}"

	// Bind AD groups to namespace
	if (profile.deploy.create_namespace.admin_group != null) {
		sh "oc policy add-role-to-group atlas:project-admin ${profile.deploy.create_namespace.admin_group} -n ${profile.deploy.namespace} || true"
	}

	if (fileExists('deployment/dev-namespace.yaml')) {
		// Setup namespace secrets
		sh 'cat \"deployment/dev-namespace.yaml\" | envsubst | oc apply -f -'
	}
}
