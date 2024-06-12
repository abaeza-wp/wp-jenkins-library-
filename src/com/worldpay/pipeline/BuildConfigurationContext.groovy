package com.worldpay.pipeline

class BuildConfigurationContext {

	private static BuildConfig currentBuildConfig

	/**
	 * For supported regions see: https://github.worldpay.com/pages/Engineering/portal/engineering/developer-platforms/kubernetes/guides/clusters/
	 * @param environment the current environment
	 * @param awsRegion the target aws region
	 */
	static BuildConfig getClusterInformationForAwsRegion(String environment, String awsRegion) {
		switch ("${environment}-${awsRegion}") {
			case "dev-eu-west-1":
				currentBuildConfig = new BuildConfig(
				"dev-euwest1",
				new GkopCluster("dev", "eu-west-1", "7z55k", "api.eu-west-1-7z55k.dev.msp.worldpay.io:6443"))
				break

			case "dev-us-east-1":
				currentBuildConfig = new BuildConfig(
				"dev-useast1",
				new GkopCluster("dev", "us-east-1", "2m2bt", "api.us-east-1-2m2bt.dev.msp.worldpay.io:6443"))
				break
			case "staging-eu-west-1":
				currentBuildConfig = new BuildConfig(
				"staging-euwest1",
				new GkopCluster("stage", "eu-west-1", "hf2js", "api.eu-west-1-hf2js.stage.msp.worldpay.io:443"))
				break
			case "staging-us-east-1":
				currentBuildConfig = new BuildConfig(
				"staging-useast1",
				new GkopCluster("stage", "us-east-1", "aq0mb", "api.us-east-1-aq0mb.stage.msp.worldpay.io:443"))
				break
			case "prod-eu-west-1":
				currentBuildConfig = new BuildConfig(
				"prod-euwest1",
				new GkopCluster("prod", "eu-west-1", "i8tjd", "api.eu-west-1-i8tjd.prod.msp.worldpay.io:443"))
				break
			case "prod-us-east-1":
				currentBuildConfig = new BuildConfig(
				"prod-useast1",
				new GkopCluster("prod", "us-east-1", "ob4yk", "api.us-east-1-ob4yk.prod.msp.worldpay.io:443"))
				break
			default:
				error "Unsupported GKOP aws region: ${awsRegion} for Environment ${environment}, If you think this is a mistake please consider raising an issue."
		}

		return currentBuildConfig
	}

	static BuildConfig getCurrentBuildConfig() {
		return currentBuildConfig
	}
}
