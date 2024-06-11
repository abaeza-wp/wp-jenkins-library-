def call(String type, String tenant, String component, Closure body) {

	switch (type) {
		case "java":
			withSpringBootPipeline(type, tenant, component) {}
			break
		case "java-improved-flow":
			withSpringBootPipelineImprovedFlow(type, tenant, component) {}
			break
		default:
			error "ERROR: Unsupported pipeline type used '${type}'"
			break
	}
	body.call()
}
