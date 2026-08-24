package rhx.lazy.integration.processor

public object DataGenContributionValidator {
    public fun validate(
        declaredIntegrations: Set<String>,
        contributionIds: Collection<String>,
    ) {
        val contributedIntegrations = contributionIds.toSet()
        val unknown = contributedIntegrations - declaredIntegrations
        require(unknown.isEmpty()) { "DataGen contributions reference integrations not enabled by the DSL: ${unknown.sorted()}" }
        val missing = declaredIntegrations - contributedIntegrations
        require(missing.isEmpty()) { "DataGen-enabled integrations have no contribution: ${missing.sorted()}" }
    }
}
