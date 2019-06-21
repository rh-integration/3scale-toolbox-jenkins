#!groovy

package com.redhat

enum ThreescaleSecurityScheme {
  OPEN,
  APIKEY,
  OIDC;

  // Avoid Jenkins sandbox restriction on java.util.LinkedHashMap
  // by providing an empty constructor
  public ThreescaleSecurityScheme() {}
}

class OpenAPI2 {
    String filename
    def content
    String version
    String majorVersion
    ThreescaleSecurityScheme securityScheme
    boolean validateOAS = true

    OpenAPI2(Map conf) {
        assert conf.filename != null
        this.filename = conf.filename
    }

    void parseOpenAPISpecificationFile() {
        this.content = new Util().readOpenAPISpecificationFile(this.filename)
        assert content.swagger == "2.0"
        this.version = content.info.version
        assert this.version != null
        this.majorVersion = version.tokenize(".")[0]

        String securitySchemeName = null
        if (content.security != null && content.security.size() == 1) {
            Set securitySchemes = content.security[0].keySet()
            if (securitySchemes.size() == 1) {
                securitySchemeName = securitySchemes.first()
            } else {
                throw new Exception("Cannot handle OpenAPI Specifications with multiple security requirements or no global requirement. Found ${content.security.size()}/${securitySchemes.size()} security requirements.")
            }

        } else if ((content.security == null || content.security.size() == 0)
                && (content.securityDefinitions == null || content.securityDefinitions.size() == 0)) {

            // To make it explicit that this is an Open API, we require no security requirement
            // and no security definition (better be safe than sorry...)
            this.securityScheme = ThreescaleSecurityScheme.OPEN
        } else {
            throw new Exception("Cannot handle OpenAPI Specifications with multiple security requirements or no global requirement. Found ${content.security != null ? content.security.size() : "no"} security requirements.")
        }

        if (securitySchemeName != null) {
            if (content.securityDefinitions != null
             && content.securityDefinitions.get(securitySchemeName) != null) {

                Map securityScheme = content.securityDefinitions.get(securitySchemeName)
                String securityType = securityScheme.type

                if (securityType == "oauth2") {
                    this.securityScheme = ThreescaleSecurityScheme.OIDC
                } else if (securityType == "apiKey") {
                    this.securityScheme = ThreescaleSecurityScheme.APIKEY
                } else {
                    throw new Exception("Cannot handle OpenAPI Specifications with security scheme: ${securityType}")
                }
            } else {
                throw new Exception("Cannot find security scheme ${securitySchemeName} in OpenAPI Specifications")
            }
        } // else: this is an Open API
    }
}
