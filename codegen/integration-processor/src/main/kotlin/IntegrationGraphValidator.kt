package rhx.lazy.integration.processor

public data class IntegrationGraphNode(
    val id: String,
    val dependencies: Set<String>,
)

public object IntegrationGraphValidator {
    public fun topologicalOrder(nodes: Collection<IntegrationGraphNode>): List<String> {
        val byId = nodes.associateBy(IntegrationGraphNode::id)
        require(byId.size == nodes.size) { "Integration ids must be unique" }
        nodes.forEach { node ->
            val unknown = node.dependencies - byId.keys
            require(unknown.isEmpty()) { "Integration ${node.id} has unknown dependencies: ${unknown.sorted()}" }
        }

        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val ordered = mutableListOf<String>()

        fun visit(id: String) {
            if (id in visited) return
            require(visiting.add(id)) { "Integration dependency cycle contains $id" }
            byId
                .getValue(id)
                .dependencies
                .sorted()
                .forEach(::visit)
            visiting.remove(id)
            visited.add(id)
            ordered.add(id)
        }

        byId.keys.sorted().forEach(::visit)
        return ordered
    }
}
