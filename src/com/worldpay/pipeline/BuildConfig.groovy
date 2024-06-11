package com.worldpay.pipeline

class BuildConfig {
	private Object profileName
	private GkopCluster cluster

	BuildConfig(String profileName, GkopCluster cluster) {
		this.profileName = profileName
		this.cluster = cluster
	}

	Object getProfileName() {
		return profileName
	}

	GkopCluster getCluster() {
		return cluster
	}
}
