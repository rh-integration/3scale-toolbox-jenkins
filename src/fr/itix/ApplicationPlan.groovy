#!groovy

package fr.itix

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
}