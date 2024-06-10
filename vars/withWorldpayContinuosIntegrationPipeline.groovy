def call(String type, String tenant, String component, Closure body) {

    switch (type) {
        case "java" -> withSpringBootPipeline(type, tenant, component) {}
        default -> error "ERROR: Unsupported pipeline type used '${type}'"
    }
    body.call()
}
