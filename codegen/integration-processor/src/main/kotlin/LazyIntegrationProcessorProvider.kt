package rhx.lazy.integration.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

public class LazyIntegrationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = LazyIntegrationProcessor(environment)
}

private class LazyIntegrationProcessor(
    environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    private val options = environment.options
    private val logger: KSPLogger = environment.logger
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val common = resolver.annotatedClasses(COMMON_ANNOTATION)
        val client = resolver.annotatedClasses(CLIENT_ANNOTATION)
        val framework = resolver.annotatedClasses(FRAMEWORK_ANNOTATION)
        val all = common + client + framework
        if (all.isEmpty() && options[OPTION_ID] != null) {
            generated = true
            return emptyList()
        }
        val deferred = all.filterNot(KSAnnotated::validate)
        if (deferred.isNotEmpty()) return deferred

        val integrationId = options[OPTION_ID]
        if (integrationId == null) {
            val catalogSpec = options[OPTION_CATALOG]
            if (catalogSpec == null) {
                generateDataGenCatalog(resolver)
            } else {
                generateIntegrationCatalog(resolver, catalogSpec)
            }
            generated = true
            return emptyList()
        }

        val owner = options.getValue(OPTION_OWNER)
        val side = options.getValue(OPTION_SIDE)
        validateEntrypoints(owner, side, common, client, framework)
        generateBridge(
            integrationId = integrationId,
            owner = owner,
            side = side,
            requiredMods = options[OPTION_REQUIRED_MODS].orEmpty().csv(),
            dependencies = options[OPTION_DEPENDENCIES].orEmpty().csv(),
            dataGen = options[OPTION_DATAGEN].toBoolean(),
            common = common.singleOrNull(),
            client = client.singleOrNull(),
            framework = framework,
            sourceFiles = all.mapNotNull(KSClassDeclaration::containingFile).distinct(),
        )
        generated = true
        return emptyList()
    }

    private fun validateEntrypoints(
        owner: String,
        side: String,
        common: List<KSClassDeclaration>,
        client: List<KSClassDeclaration>,
        framework: List<KSClassDeclaration>,
    ) {
        if (owner == "LAZY") {
            if (common.size != 1) logger.error("Lazy-managed integration must declare exactly one common entrypoint")
            if (framework.isNotEmpty()) logger.error("Lazy-managed integration cannot declare framework entrypoints")
            common.forEach { it.requireSupertype(COMMON_INTERFACE) }
            client.forEach { it.requireSupertype(CLIENT_INTERFACE) }
            if (client.size > 1) logger.error("Integration may declare at most one client entrypoint")
            if (side == "CLIENT" && common.isNotEmpty()) logger.error("Client-only integration cannot declare a common entrypoint")
        } else {
            if (common.isNotEmpty() || client.isNotEmpty()) {
                logger.error("Framework-managed integration must use @LazyFrameworkEntrypoint")
            }
            if (framework.isEmpty()) logger.error("Framework-managed integration must declare at least one framework entrypoint")
            val keys = framework.map(::frameworkKey)
            if (keys.any(String::isBlank)) logger.error("Framework entrypoint keys must not be blank")
            if (keys.distinct().size != keys.size) logger.error("Framework entrypoint keys must be unique")
            framework.forEach { declaration ->
                when (owner) {
                    "JADE" -> {
                        declaration.requireSupertype(JADE_INTERFACE)
                        declaration.requireAnnotation(JADE_ANNOTATION)
                    }
                    "JEI" -> {
                        declaration.requireSupertype(JEI_INTERFACE)
                        declaration.requireAnnotation(JEI_ANNOTATION)
                    }
                    "KUBEJS" -> declaration.requireSupertype(KUBEJS_INTERFACE)
                    else -> logger.error("Unknown integration owner: $owner", declaration)
                }
            }
        }
    }

    private fun generateBridge(
        integrationId: String,
        owner: String,
        side: String,
        requiredMods: List<String>,
        dependencies: List<String>,
        dataGen: Boolean,
        common: KSClassDeclaration?,
        client: KSClassDeclaration?,
        framework: List<KSClassDeclaration>,
        sourceFiles: List<KSFile>,
    ) {
        val simpleName = integrationId.toPascalCase() + "IntegrationBridge"
        val packageName = "rhx.lazy.generated.integration.${integrationId.toPackageSegment()}"
        codeGenerator
            .createNewFile(Dependencies(false, *sourceFiles.toTypedArray()), packageName, simpleName)
            .bufferedWriter()
            .use { writer ->
                writer.appendLine("package $packageName")
                writer.appendLine()
                writer.appendLine("public object $simpleName {")
                writer.appendLine("    public const val ID: String = \"$integrationId\"")
                writer.appendLine("    public const val OWNER: String = \"$owner\"")
                writer.appendLine("    public const val SIDE: String = \"$side\"")
                writer.appendLine("    public const val DATA_GEN: Boolean = $dataGen")
                writer.appendLine("    public val REQUIRED_MODS: Set<String> = ${requiredMods.asKotlinSet()}")
                writer.appendLine("    public val DEPENDENCIES: Set<String> = ${dependencies.asKotlinSet()}")
                common?.let { declaration ->
                    writer.appendLine(
                        "    public fun createCommon(): rhx.lazy.integration.api.CommonIntegration = ${declaration.qualifiedName!!.asString()}",
                    )
                }
                client?.let { declaration ->
                    writer.appendLine(
                        "    public fun createClient(): rhx.lazy.integration.api.ClientIntegration = ${declaration.qualifiedName!!.asString()}",
                    )
                }
                writer.appendLine(
                    "    public val FRAMEWORK_ENTRYPOINTS: List<String> = ${framework.map {
                        it.qualifiedName!!.asString()
                    }.asKotlinList()}",
                )
                writer.appendLine("}")
            }

        if (owner == "KUBEJS") {
            codeGenerator
                .createNewFile(Dependencies(false, *sourceFiles.toTypedArray()), "", "kubejs.plugins", "txt")
                .bufferedWriter()
                .use { writer -> framework.forEach { writer.appendLine(it.qualifiedName!!.asString()) } }
        }
    }

    private fun generateDataGenCatalog(resolver: Resolver) {
        val contributions = resolver.annotatedClasses(DATAGEN_ANNOTATION)
        val declaredIntegrations = options[OPTION_DATAGEN_CATALOG]?.csv()?.toSet().orEmpty()
        if (contributions.isEmpty()) {
            if (declaredIntegrations.isNotEmpty()) {
                logger.error("DataGen-enabled integrations have no contribution: ${declaredIntegrations.sorted()}")
            }
            return
        }
        val deferred = contributions.filterNot(KSAnnotated::validate)
        if (deferred.isNotEmpty()) return
        val contributionIds = contributions.map(::dataGenIntegrationId)
        try {
            DataGenContributionValidator.validate(declaredIntegrations, contributionIds)
        } catch (exception: IllegalArgumentException) {
            logger.error(exception.message.orEmpty())
            return
        }
        contributions.forEach { contribution ->
            val gatherFunctions =
                contribution.declarations
                    .filterIsInstance<KSFunctionDeclaration>()
                    .filter { function -> function.simpleName.asString() == "gatherData" }
                    .toList()
            if (gatherFunctions.size != 1) {
                logger.error("DataGen contribution must declare exactly one gatherData function", contribution)
            }
        }
        val sourceFiles = contributions.mapNotNull(KSClassDeclaration::containingFile).distinct()
        codeGenerator
            .createNewFile(
                Dependencies(true, *sourceFiles.toTypedArray()),
                "rhx.lazy.generated.datagen",
                "GeneratedDataGenCatalog",
            ).bufferedWriter()
            .use { writer ->
                writer.appendLine("package rhx.lazy.generated.datagen")
                writer.appendLine()
                writer.appendLine("public object GeneratedDataGenCatalog {")
                writer.appendLine("    public fun gather(event: net.neoforged.neoforge.data.event.GatherDataEvent) {")
                contributions.sortedBy { it.qualifiedName!!.asString() }.forEach { contribution ->
                    writer.appendLine("        ${contribution.qualifiedName!!.asString()}.gatherData(event)")
                }
                writer.appendLine("    }")
                writer.appendLine("}")
            }
    }

    private fun generateIntegrationCatalog(
        resolver: Resolver,
        catalogSpec: String,
    ) {
        val descriptors =
            catalogSpec
                .lineSequence()
                .filter(String::isNotBlank)
                .map(::parseDescriptor)
                .toList()
        val byId = descriptors.associateBy(IntegrationDescriptor::id)
        val order =
            try {
                IntegrationGraphValidator.topologicalOrder(
                    descriptors.map { descriptor -> IntegrationGraphNode(descriptor.id, descriptor.dependencies) },
                )
            } catch (exception: IllegalArgumentException) {
                logger.error(exception.message.orEmpty())
                return
            }

        descriptors.forEach { descriptor ->
            descriptor.dependencies.forEach { dependencyId ->
                val dependency = byId.getValue(dependencyId)
                if (descriptor.side == "BOTH" && dependency.side == "CLIENT") {
                    logger.error("Integration ${descriptor.id} cannot depend on client-only integration $dependencyId")
                }
                val missingRequiredMods = dependency.requiredModIds - descriptor.requiredModIds
                if (missingRequiredMods.isNotEmpty()) {
                    logger.error(
                        "Integration ${descriptor.id} does not preserve the hard dependency activation closure of " +
                            "$dependencyId: ${missingRequiredMods.sorted()}",
                    )
                }
            }
        }

        val bridges =
            descriptors.associateWith { descriptor ->
                resolver.getClassDeclarationByName(resolver.getKSNameFromString(descriptor.bridgeClass)).also { declaration ->
                    if (declaration == null) logger.error("Missing generated bridge ${descriptor.bridgeClass}")
                }
            }
        val orderedDescriptors = order.map(byId::getValue)
        val commonDescriptors =
            orderedDescriptors.filter { descriptor -> descriptor.owner == "LAZY" && descriptor.side == "BOTH" }
        commonDescriptors.forEach { descriptor ->
            if (bridges[descriptor]?.hasFunction("createCommon") != true) {
                logger.error("Bridge ${descriptor.bridgeClass} does not expose a common integration entrypoint")
            }
        }
        val clientDescriptors =
            orderedDescriptors.filter { descriptor -> bridges[descriptor]?.hasFunction("createClient") == true }

        generateCommonCatalog(commonDescriptors)
        generateClientCatalog(clientDescriptors)
        generateIntegrationManifest(orderedDescriptors)
    }

    private fun generateCommonCatalog(descriptors: List<IntegrationDescriptor>) {
        codeGenerator
            .createNewFile(Dependencies(true), GENERATED_CATALOG_PACKAGE, "GeneratedCommonIntegrationCatalog")
            .bufferedWriter()
            .use { writer ->
                writer.appendLine("package $GENERATED_CATALOG_PACKAGE")
                writer.appendLine()
                writer.appendLine("public object GeneratedCommonIntegrationCatalog {")
                writer.appendLine("    private data class Entry(")
                writer.appendLine("        val id: String,")
                writer.appendLine("        val requiredMods: Set<String>,")
                writer.appendLine("        val dependencies: Set<String>,")
                writer.appendLine("        val create: () -> rhx.lazy.integration.api.CommonIntegration,")
                writer.appendLine("    )")
                writer.appendLine()
                writer.appendLine("    private val entries: List<Entry> = listOf(")
                descriptors.forEach { descriptor ->
                    writer.appendLine("        Entry(")
                    writer.appendLine("            id = \"${descriptor.id}\",")
                    writer.appendLine("            requiredMods = ${descriptor.requiredModIds.sorted().asKotlinSet()},")
                    writer.appendLine("            dependencies = ${descriptor.dependencies.sorted().asKotlinSet()},")
                    writer.appendLine("            create = ${descriptor.bridgeClass}::createCommon,")
                    writer.appendLine("        ),")
                }
                writer.appendLine("    )")
                writer.appendLine()
                writer.appendLine("    public fun registerConfig(")
                writer.appendLine("        context: rhx.lazy.integration.api.IntegrationConfigContext,")
                writer.appendLine("        loadedMods: Set<String>,")
                writer.appendLine("    ) = activeEntries(loadedMods).forEach { entry ->")
                writer.appendLine("        runPhase(entry.id, \"registerConfig\") { entry.create().registerConfig(context) }")
                writer.appendLine("    }")
                writer.appendLine()
                writer.appendLine("    public fun install(")
                writer.appendLine("        context: rhx.lazy.integration.api.IntegrationCommonContext,")
                writer.appendLine("        loadedMods: Set<String>,")
                writer.appendLine("    ) = activeEntries(loadedMods).forEach { entry ->")
                writer.appendLine("        runPhase(entry.id, \"common install\") { entry.create().install(context) }")
                writer.appendLine("    }")
                writer.appendLine()
                writer.appendLine("    private fun activeEntries(loadedMods: Set<String>): List<Entry> {")
                writer.appendLine("        val active = entries.filter { entry -> loadedMods.containsAll(entry.requiredMods) }")
                writer.appendLine("        val activeIds = active.mapTo(mutableSetOf()) { entry -> entry.id }")
                writer.appendLine("        check(active.all { entry -> activeIds.containsAll(entry.dependencies) }) {")
                writer.appendLine("            \"Active integration set violates its generated dependency closure\"")
                writer.appendLine("        }")
                writer.appendLine("        return active")
                writer.appendLine("    }")
                writer.appendLine()
                writer.appendLine("    private inline fun runPhase(id: String, phase: String, action: () -> Unit) {")
                writer.appendLine("        try {")
                writer.appendLine("            action()")
                writer.appendLine("        } catch (failure: Throwable) {")
                writer.appendLine(
                    "            throw IllegalStateException(\"Lazy integration '\" + id + \"' failed during \" + phase, failure)",
                )
                writer.appendLine("        }")
                writer.appendLine("    }")
                writer.appendLine("}")
            }
    }

    private fun generateClientCatalog(descriptors: List<IntegrationDescriptor>) {
        codeGenerator
            .createNewFile(Dependencies(true), GENERATED_CLIENT_CATALOG_PACKAGE, "GeneratedClientIntegrationCatalog")
            .bufferedWriter()
            .use { writer ->
                writer.appendLine("package $GENERATED_CLIENT_CATALOG_PACKAGE")
                writer.appendLine()
                writer.appendLine("public object GeneratedClientIntegrationCatalog {")
                writer.appendLine("    public fun install(")
                writer.appendLine("        context: rhx.lazy.integration.api.IntegrationClientContext,")
                writer.appendLine("        loadedMods: Set<String>,")
                writer.appendLine("    ) {")
                descriptors.forEach { descriptor ->
                    writer.appendLine("        if (loadedMods.containsAll(${descriptor.requiredModIds.sorted().asKotlinSet()})) {")
                    writer.appendLine("            try {")
                    writer.appendLine("                ${descriptor.bridgeClass}.createClient().install(context)")
                    writer.appendLine("            } catch (failure: Throwable) {")
                    writer.appendLine(
                        "                throw IllegalStateException(\"Lazy integration '${descriptor.id}' failed during client install\", failure)",
                    )
                    writer.appendLine("            }")
                    writer.appendLine("        }")
                }
                writer.appendLine("    }")
                writer.appendLine("}")
            }
    }

    private fun generateIntegrationManifest(descriptors: List<IntegrationDescriptor>) {
        codeGenerator
            .createNewFile(Dependencies(true), "META-INF.lazy", "integrations", "json")
            .bufferedWriter()
            .use { writer ->
                writer.appendLine("{")
                writer.appendLine("  \"integrations\": [")
                descriptors.forEachIndexed { index, descriptor ->
                    writer.append("    {\"id\":\"${descriptor.id}\",\"owner\":\"${descriptor.owner}\",\"side\":\"${descriptor.side}\"")
                    writer.append(",\"requiredMods\":${descriptor.requiredMods.sorted().asJsonArray()}")
                    writer.append(",\"dependencies\":${descriptor.dependencies.sorted().asJsonArray()}")
                    writer.append(",\"dataGen\":${descriptor.dataGen}}")
                    writer.appendLine(if (index == descriptors.lastIndex) "" else ",")
                }
                writer.appendLine("  ]")
                writer.appendLine("}")
            }
    }

    private fun parseDescriptor(line: String): IntegrationDescriptor {
        val fields = line.split('~')
        require(fields.size == 7) { "Invalid integration catalog descriptor: $line" }
        return IntegrationDescriptor(
            id = fields[0],
            owner = fields[1],
            side = fields[2],
            requiredMods = fields[3].csv().toSet(),
            dependencies = fields[4].csv().toSet(),
            dataGen = fields[5].toBooleanStrict(),
            bridgeClass = fields[6],
        )
    }

    private fun KSClassDeclaration.hasFunction(name: String): Boolean =
        declarations.filterIsInstance<KSFunctionDeclaration>().any { declaration -> declaration.simpleName.asString() == name }

    private fun Resolver.annotatedClasses(annotation: String): List<KSClassDeclaration> =
        getSymbolsWithAnnotation(annotation)
            .mapNotNull { symbol ->
                val declaration = symbol as? KSClassDeclaration
                if (declaration == null) logger.error("@$annotation may only annotate classes", symbol)
                declaration
            }.toList()

    private fun KSClassDeclaration.requireSupertype(qualifiedName: String) {
        if (getAllSuperTypes().none { it.declaration.qualifiedName?.asString() == qualifiedName }) {
            logger.error("${this.qualifiedName?.asString()} must implement $qualifiedName", this)
        }
    }

    private fun KSClassDeclaration.requireAnnotation(qualifiedName: String) {
        if (annotations.none {
                it.annotationType
                    .resolve()
                    .declaration.qualifiedName
                    ?.asString() == qualifiedName
            }
        ) {
            logger.error("${this.qualifiedName?.asString()} must also declare @$qualifiedName", this)
        }
    }

    private fun frameworkKey(declaration: KSClassDeclaration): String =
        declaration.annotations
            .first {
                it.annotationType
                    .resolve()
                    .declaration.qualifiedName
                    ?.asString() == FRAMEWORK_ANNOTATION
            }.arguments
            .firstOrNull { it.name?.asString() == "key" }
            ?.value
            ?.toString()
            .orEmpty()

    private fun dataGenIntegrationId(declaration: KSClassDeclaration): String =
        declaration.annotations
            .first {
                it.annotationType
                    .resolve()
                    .declaration.qualifiedName
                    ?.asString() == DATAGEN_ANNOTATION
            }.arguments
            .first { argument -> argument.name?.asString() == "integrationId" }
            .value
            .toString()

    private fun String.csv(): List<String> = split(',').map(String::trim).filter(String::isNotEmpty)

    private fun String.toPackageSegment(): String = replace('-', '_').replace('.', '_')

    private fun String.toPascalCase(): String = split('-', '_', '.').joinToString("") { part -> part.replaceFirstChar(Char::uppercaseChar) }

    private fun List<String>.asKotlinSet(): String = joinToString(prefix = "setOf(", postfix = ")") { "\"$it\"" }

    private fun List<String>.asKotlinList(): String = joinToString(prefix = "listOf(", postfix = ")") { "\"$it\"" }

    private fun List<String>.asJsonArray(): String = joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

    private data class IntegrationDescriptor(
        val id: String,
        val owner: String,
        val side: String,
        val requiredMods: Set<String>,
        val dependencies: Set<String>,
        val dataGen: Boolean,
        val bridgeClass: String,
    ) {
        val requiredModIds: Set<String> =
            requiredMods.mapTo(mutableSetOf()) { requirement -> requirement.substringBefore('|') }
    }

    private companion object {
        const val COMMON_ANNOTATION = "rhx.lazy.integration.annotation.LazyCommonEntrypoint"
        const val CLIENT_ANNOTATION = "rhx.lazy.integration.annotation.LazyClientEntrypoint"
        const val FRAMEWORK_ANNOTATION = "rhx.lazy.integration.annotation.LazyFrameworkEntrypoint"
        const val DATAGEN_ANNOTATION = "rhx.lazy.integration.annotation.LazyDataGenContribution"
        const val COMMON_INTERFACE = "rhx.lazy.integration.api.CommonIntegration"
        const val CLIENT_INTERFACE = "rhx.lazy.integration.api.ClientIntegration"
        const val JADE_INTERFACE = "snownee.jade.api.IWailaPlugin"
        const val JADE_ANNOTATION = "snownee.jade.api.WailaPlugin"
        const val JEI_INTERFACE = "mezz.jei.api.IModPlugin"
        const val JEI_ANNOTATION = "mezz.jei.api.JeiPlugin"
        const val KUBEJS_INTERFACE = "dev.latvian.mods.kubejs.plugin.KubeJSPlugin"
        const val OPTION_ID = "lazy.integration.id"
        const val OPTION_OWNER = "lazy.integration.owner"
        const val OPTION_SIDE = "lazy.integration.side"
        const val OPTION_REQUIRED_MODS = "lazy.integration.requiredMods"
        const val OPTION_DEPENDENCIES = "lazy.integration.dependencies"
        const val OPTION_DATAGEN = "lazy.integration.dataGen"
        const val OPTION_CATALOG = "lazy.integration.catalog"
        const val OPTION_DATAGEN_CATALOG = "lazy.datagen.integrations"
        const val GENERATED_CATALOG_PACKAGE = "rhx.lazy.generated.integration.catalog"
        const val GENERATED_CLIENT_CATALOG_PACKAGE = "rhx.lazy.generated.integration.client"
    }
}
