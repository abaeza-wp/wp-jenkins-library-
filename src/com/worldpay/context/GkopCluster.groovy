package com.worldpay.context

class GkopCluster {

    private String environment
    private String awsRegion
    private String clusterName
    private String api
    private String imageRegistry

    GkopCluster(String environment, String awsRegion, String clusterName, String api, String imageRegistry) {
        this.environment = environment
        this.awsRegion = awsRegion
        this.clusterName = clusterName
        this.api = api
        this.imageRegistry = imageRegistry
    }

    String getEnvironment() {
        return environment
    }

    String isDev() {
        return environment == 'dev'
    }

    String getAwsRegion() {
        return awsRegion
    }

    String getClusterName() {
        return clusterName
    }

    String getApi() {
        return api
    }

    String getImageRegistry() {
        return imageRegistry
    }

}
