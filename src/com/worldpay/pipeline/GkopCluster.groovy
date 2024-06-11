package com.worldpay.pipeline

class GkopCluster {
	private String environment
	private String awsRegion
	private String clusterName
	private String api

	GkopCluster(String environment, String awsRegion, String clusterName, String api) {
		this.environment = environment
		this.awsRegion = awsRegion
		this.clusterName = clusterName
		this.api = api
	}

	String getEnvironment() {
		return environment
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
}
