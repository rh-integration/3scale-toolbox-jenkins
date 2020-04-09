#!groovy

package com.redhat

abstract class OpenAPI {
    String filename
    def content
    String version
    String majorVersion
    ThreescaleSecurityScheme securityScheme
    boolean validateOAS = true

    OpenAPI(Map conf) {
        assert conf.filename != null
        assert conf.content != null
        this.filename = conf.filename
        this.content = conf.content
    }

    String getUpdatedContent() {
        Util util = new Util()
        String baseName = "updated-" + util.basename(this.filename)
        util.removeFile(baseName)
        util.writeOpenAPISpecificationFile(baseName, this.content)
        return util.readFile(baseName)
    }

    void updateTitleWithEnvironmentAndVersion(String environmentName) {
        String title = this.content.info.title as String
        String version = this.version as String
        String newTitle = null
        if (environmentName != null) {
            newTitle = "${title} (${environmentName.toUpperCase()}, v${version})"
        } else {
            newTitle = "${title} (v${version})"
        }
        this.content.info.title = newTitle
    }
    
    abstract void parseOpenAPISpecificationFile()
}
