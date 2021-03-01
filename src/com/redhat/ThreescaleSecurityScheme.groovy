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
