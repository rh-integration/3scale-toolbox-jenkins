#!groovy

package fr.itix

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

  def runToolbox(Map conf) {
    return toolbox.runToolbox(this, conf)
  }

  String getToolboxVersion() {
    def result = runToolbox(this,
                            [ commandLine: "3scale -v",
                              jobName: "version" ])
    return result.stdout
  }
}
