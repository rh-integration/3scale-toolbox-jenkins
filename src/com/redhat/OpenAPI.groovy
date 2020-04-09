#!groovy

package com.redhat

interface OpenAPI {
    String getUpdatedContent()
    void updateTitleWithEnvironmentAndVersion(String environmentName)
    void parseOpenAPISpecificationFile()
}
