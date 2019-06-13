# Jenkins Shared Library to call the 3scale toolbox

This [Jenkins Shared Library](https://jenkins.io/doc/book/pipeline/shared-libraries/) helps pipelines writers to call the 3scale toolbox.

## How to use this shared library

First, reference this library from your Jenkins pipeline by adding to the beginning of your pipeline:

```groovy
library identifier: '3scale-toolbox-jenkins@master',
        retriever: modernSCM([$class: 'GitSCMSource',
                              remote: 'https://github.com/rh-integration/3scale-toolbox-jenkins.git'])
```

Declare a global variable that will hold the `ThreescaleService` object so that you can use it from the different stages of your pipeline.

```groovy
def service = null
```

Create the `ThreescaleService` with all the relevant information:

```groovy
  stage("Prepare") {
    service = toolbox.prepareThreescaleService(
        openapi: [ filename: "swagger.json" ],
        environment: [ baseSystemName: "my_service" ],
        toolbox: [ openshiftProject: "toolbox",
                   destination: "3scale-tenant",
                   secretName: "3scale-toolbox" ],
        service: [:],
        applicationPlans: [
          [ systemName: "test", name: "Test", defaultPlan: true, published: true ],
          [ systemName: "silver", name: "Silver" ],
          [ systemName: "gold", name: "Gold" ],
        ]
    )

    echo "toolbox version = " + service.toolbox.getToolboxVersion()
  }
```

- `openapi.filename` is the path to the file containing the OpenAPI Specification
- `environment.baseSystemName` is use to compute the final system_name, based on the environment name (`environment.environmentName`) and the API major version (from the OpenAPI Specification field `info.version`)
- `toolbox.openshiftProject` is the OpenShift project in which Kubernetes Jobs will be created
- `toolbox.secretName` is the name of the [Kubernetes Secret](https://kubernetes.io/docs/concepts/configuration/secret/) containing the [3scale_toolbox configuration file](https://github.com/3scale/3scale_toolbox/blob/master/docs/remotes.md#options)
- `toolbox.destination` is the name of the [3scale_toolbox remote](https://github.com/3scale/3scale_toolbox/blob/master/docs/remotes.md)
- `applicationPlans` is a list of Application Plans to create

Create the corresponding OpenShift project:

```sh
oc new-project toolbox
```

Generate the toolbox configuration file and create the Kubernetes Secret:

```sh
3scale remote add 3scale-tenant https://$TOKEN@$TENANT.3scale.net/
oc create secret generic 3scale-toolbox --from-file=$HOME/.3scalerc.yaml
```

Add a stage to provision the service in 3scale:

```groovy
  stage("Import OpenAPI") {
    service.importOpenAPI()
    echo "Service with system_name ${service.environment.targetSystemName} created !"
  }
```

Add a stage to create the Application Plans:

```groovy
  stage("Create an Application Plan") {
    service.applyApplicationPlans()
  }
```
