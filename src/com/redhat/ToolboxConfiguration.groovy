#!groovy

package com.redhat

class ToolboxConfiguration {
  String openshiftProject
  String destination
  String secretName
  String image = "quay.io/redhat/3scale-toolbox:v0.10.0"
  String backoffLimit = 2 // three attempts (one first try + two retries)
  String imagePullPolicy = "IfNotPresent"
  int activeDeadlineSeconds = 90
  String JOB_BASE_NAME
  String BUILD_NUMBER
  Toolbox toolbox = new Toolbox()
  boolean insecure = false
  boolean verbose = false

  def runToolbox(Map conf) {
    return toolbox.runToolbox(this, conf)
  }

  String getToolboxVersion() {
    def result = toolbox.runToolbox(this,
                                    [ commandLine: "3scale -v",
                                      jobName: "version" ])
    return result.stdout
  }

  String getGlobalToolboxOptions() {
      def options = ""
      if (this.insecure) {
          options += "-k "
      }
      if (this.verbose) {
          options += "--verbose "
      }
      return options
  }

}
