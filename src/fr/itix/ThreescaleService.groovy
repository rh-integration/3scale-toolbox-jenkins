#!groovy

package fr.itix

class ThreescaleService {
  OpenAPI2 openapi
  List<ApplicationPlan> applicationPlans
  ToolboxConfiguration toolbox
  ThreescaleEnvironment environment

  void importOpenAPI() {
    if (this.environment.stagingPublicBaseURL != null || this.environment.productionPublicBaseURL != null) {
      // See https://issues.jboss.org/browse/THREESCALE-2607
      throw new Exception("NOT_IMPLEMENTED")
    }
    // Compute the target system_name
    this.environment.targetSystemName = (this.environment.environmentName != null ? "${this.environment.environmentName}_" : "") + this.environment.baseSystemName + "_${this.openapi.majorVersion}"

    def baseName = basename(this.openapi.filename)
    def globalOptions = getGlobalToolboxOptions(conf)
    def commandLine = "3scale import openapi ${globalOptions} -t ${this.environment.targetSystemName} -d ${this.toolbox.destination} /artifacts/${baseName}"
    toolbox.runToolbox(commandLine: commandLine,
                       jobName: "import",
                       openAPI: [
                         "filename": baseName,
                         "content": readFile(this.openapi.filename)
                       ])
  }
  
  static String getGlobalToolboxOptions(Map conf) {
      def options = ""
      if (conf.insecure != null && conf.insecure) {
          options += "-k "
      }
      if (conf.verbose != null && conf.verbose) {
          options += "--verbose "
      }
      return options
  }

  static String basename(path) {
      def filename = path.drop(path.lastIndexOf("/") != -1 ? path.lastIndexOf("/") + 1 : 0)
      filename = filename.replaceAll("[^-._a-zA-Z0-9]", "_")
      return filename
  }

}

