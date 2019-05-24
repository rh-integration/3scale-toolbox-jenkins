#!groovy

package fr.itix

class ToolboxConfiguration {
  def openshiftClient
  String destination
  String secretName
  String image = "quay.io/redhat/3scale-toolbox:v0.10.0"
  String backoffLimit = 2 // three attempts (one first try + two retries)
  String imagePullPolicy = "IfNotPresent"
  int activeDeadlineSeconds = 90

  def runToolbox(Map conf) {
    def result = null

    assert conf.jobName != null
    assert conf.commandLine != null

    // replace the global openshift variable of the OpenShift Client plugin by
    // the one coming from the pipeline
    def openshift = this.openshiftClient

    def oasConfigMapName = null
    if (conf.openAPI != null) {
      oasConfigMapName = "3scale-toolbox-${JOB_BASE_NAME}-${BUILD_NUMBER}-openapi"
      echo "Creating a configMap named ${oasConfigMapName} containing the OpenAPI file..."
      createConfigMap(openshift, oasConfigMapName, [ (conf.openAPI.filename): conf.openAPI.content ])
    }

    def jobName = "${JOB_BASE_NAME}-${BUILD_NUMBER}-${conf.jobName}"
    def jobSpecs = [
      "apiVersion": "batch/v1",
      "kind": "Job",
      "metadata": [
        "name": jobName,
        "labels": [
          "build": "${JOB_BASE_NAME}-${BUILD_NUMBER}",
          "job": "${JOB_BASE_NAME}"
        ]
      ],
      "spec": [
        "backoffLimit": this.backoffLimit, 
        "activeDeadlineSeconds": this.activeDeadlineSeconds,
        "template": [
          "spec": [
            "restartPolicy": "Never",
            "containers": [
              [
                "name": "job",
                "image": this.image,
                "imagePullPolicy": this.imagePullPolicy,
                "command": [ "scl", "enable", "rh-ruby25", "/opt/rh/rh-ruby25/root/usr/local/bin/${conf.commandLine}" ],
                "volumeMounts": [
                  [
                    "mountPath": "/opt/app-root/src/",
                    "name": "toolbox-config"
                  ],
                  [
                    "mountPath": "/artifacts",
                    "name": "artifacts"
                  ]

                ]
              ]
            ],
            "volumes": [
              [
                "name": "toolbox-config"
              ],
              [
                "name": "artifacts"
              ]
            ]
          ]
        ]
      ]
    ]

    // Inject the toolbox configuration as a volume
    if (this.secretName != null) {
      jobSpecs.spec.template.spec.volumes[0].secret = [
        "secretName": this.secretName
      ]
    } else {
      jobSpecs.spec.template.spec.volumes[0].emptyDir = [:]
    }

    // Inject the OpenAPI file as a volume
    if (oasConfigMapName != null) {
      jobSpecs.spec.template.spec.volumes[1].configMap = [
        "name": oasConfigMapName
      ]
    } else {
      jobSpecs.spec.template.spec.volumes[1].emptyDir = [:]
    }

    def job = null
    try {
      job = openshift.create(jobSpecs)

      int jobTimeout = 2 + (int)(this.activeDeadlineSeconds / 60.0f)
      echo "Waiting ${jobTimeout} minutes for the job to complete..."
      timeout(jobTimeout) {
        // Wait for the job to complete, either Succeeded or Failed
        job.watch {
          def jobStatus = getJobStatus(it.object())
          echo "Job ${it.name()}: succeeded = ${jobStatus.succeeded}, failed = ${jobStatus.failed}, status = ${jobStatus.status}, reason = ${jobStatus.reason}"
          
          // Exit the watch loop when the Job has one successful pod or failed
          return jobStatus.succeeded > 0 || jobStatus.status == "Failed"
        }
      }
    } finally {
      // Delete the temporary configMap containing the OAS file
      if (oasConfigMapName != null) {
        try {
          openshift.selector('configMap', oasConfigMapName).delete()
        } catch (e2) { // Best effort
          echo "cannot delete the configMap ${oasConfigMapName}: ${e2}"
        }
      }

      // If the job has been created, check its status
      if (job != null) {
        def jobStatus = getJobStatus(job.object())
        echo "job ${job.name()} has status '${jobStatus.status}' and reason '${jobStatus.reason}'"

        // Iterate over pods to find:
        //  - the pod that succeeded
        //  - as last resort, a pod that failed 
        def pods = job.related("pod")
        pods.withEach {
          if (it.object().status.phase == "Succeeded") {
            result = getPodDetails(it)
          }
          if (it.object().status.phase == "Failed" && result == null) {
            result = getPodDetails(it)
          }
        }

        // Delete the job
        try {
          openshift.selector('job', jobName).delete()
        } catch (e2) { // Best effort
          echo "cannot delete the job ${jobName}: ${e2}"
        }

        if (jobStatus.status != "Complete") {
          // If there is at least a pod that failed, show its logs
          if (result != null) {
            echo "RC: ${result.status}"
            echo "STDOUT:"
            echo "-------"
            echo result.stdout
            echo "STDERR:"
            echo "-------"
            echo result.stderr
          }

          error("job ${job.name()} exited with '${jobStatus.status}' and reason '${jobStatus.reason}'")
        }
      }
    }

    return result
  }

  String getToolboxVersion(Map conf) {
    def result = runToolbox(commandLine: "3scale -v",
                            jobName: "version")
    return result.stdout
  }

  def getJobStatus(obj) {
    return [
      "succeeded": obj.status.succeeded != null ? obj.status.succeeded : 0,
      "failed": obj.status.failed != null ? obj.status.failed : 0,
      "status": obj.status.conditions != null && obj.status.conditions.size() > 0 ? obj.status.conditions[0].type : "Unknown",
      "reason": obj.status.conditions != null && obj.status.conditions.size() > 0 ? obj.status.conditions[0].reason : ""
    ]
  }
  def getPodDetails(pod) {
    def logs = pod.logs()
    return [
      "status": logs.actions[0].status,
      "stdout": logs.actions[0].out,
      "stderr": logs.actions[0].err,
      "podPhase": pod.object().status.phase,
      "podName": pod.name()
    ]
  }

  def createConfigMap(openshift, configMapName, content) {
    def configMapSpecs = [
      "apiVersion": "v1",
      "kind": "ConfigMap",
      "metadata": [
        "name": "${configMapName}",
        "labels": [
          "job": "${JOB_BASE_NAME}",
          "build": "${JOB_BASE_NAME}-${BUILD_NUMBER}"
        ]
      ],
      "data": [:]
    ]
    content.each{ k, v -> configMapSpecs.data[k] = v }
    openshift.apply(configMapSpecs) 
  }
}
