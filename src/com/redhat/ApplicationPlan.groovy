#!groovy

package com.redhat

class ApplicationPlan {
    String systemName
    String name
    boolean approvalRequired = false
    boolean defaultPlan = false
    boolean endUserRequired = false
    boolean published = false
    BigDecimal costPerMonth = 0.0
    BigDecimal setupFee = 0.0
    BigInteger trialPeriodDays = 0
    String artefactFile

}