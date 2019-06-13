#!groovy

package com.redhat

class ThreescaleService {
  OpenAPI2 openapi
  List<ApplicationPlan> applicationPlans
  ToolboxConfiguration toolbox
  ThreescaleEnvironment environment

  void importOpenAPI() {
    Util util = new Util()

    if (this.environment.stagingPublicBaseURL != null || this.environment.productionPublicBaseURL != null) {
      // See https://issues.jboss.org/browse/THREESCALE-2607
      throw new Exception("NOT_IMPLEMENTED")
    }

    if (this.environment.privateBaseUrl != null) {
      // See https://issues.jboss.org/browse/THREESCALE-2734
      throw new Exception("NOT_IMPLEMENTED")
    }

    // Compute the target system_name
    this.environment.targetSystemName = (this.environment.environmentName != null ? "${this.environment.environmentName}_" : "") + this.environment.baseSystemName + "_${this.openapi.majorVersion}"

    def baseName = basename(this.openapi.filename)
    def globalOptions = toolbox.getGlobalToolboxOptions()
    def commandLine = "3scale import openapi ${globalOptions} -t ${this.environment.targetSystemName} -d ${this.toolbox.destination} /artifacts/${baseName}"
    toolbox.runToolbox(commandLine: commandLine,
                       jobName: "import",
                       openAPI: [
                         "filename": baseName,
                         "content": util.readFile(this.openapi.filename)
                       ])
  }

  void applyApplicationPlans() {
    def globalOptions = toolbox.getGlobalToolboxOptions()
    this.applicationPlans.each{
      def commandLine = "3scale application-plan apply ${globalOptions} ${this.toolbox.destination} ${this.environment.targetSystemName} ${it.systemName} --approval-required=${it.approvalRequired} --cost-per-month=${it.costPerMonth} --end-user-required=${it.endUserRequired} --name=${it.name} --publish=${it.published} --setup-fee=${it.setupFee} --trial-period-days=${it.trialPeriodDays}"
      if (it.defaultPlan) {
        commandLine += " --default"
      }

      toolbox.runToolbox(commandLine: commandLine,
                         jobName: "apply-application-plan-${it.systemName}")
    }
  }

  void applyApplication(Map application) {
    // See https://issues.jboss.org/browse/THREESCALE-2425
    throw new Exception("NOT_IMPLEMENTED")
  }

  Map readProxy() {
    // See https://issues.jboss.org/browse/THREESCALE-2405
    throw new Exception("NOT_IMPLEMENTED")
  }

  void promoteToProduction() {
    // See https://issues.jboss.org/browse/THREESCALE-2405
    throw new Exception("NOT_IMPLEMENTED")
  }

  static String basename(path) {
      def filename = path.drop(path.lastIndexOf("/") != -1 ? path.lastIndexOf("/") + 1 : 0)
      filename = filename.replaceAll("[^-._a-zA-Z0-9]", "_")
      return filename
  }

}

