val a = Attribute.of("a", String::class.java)
val b = Attribute.of("b", String::class.java)
val c = Attribute.of("c", String::class.java)

class AllCompatible : AttributeCompatibilityRule<String> {
    override fun execute(details: CompatibilityCheckDetails<String>) {
        details.compatible();
    }
}

class PreferExact : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        if (details.consumerValue != null && details.candidateValues.contains(details.consumerValue)) {
            details.closestMatch(details.consumerValue!!)
        }
    }
}

dependencies {
    attributesSchema {
        attribute(a)
        attribute(b)
        attribute(c) {
            compatibilityRules.add(AllCompatible::class.java)
            disambiguationRules.add(PreferExact::class.java)
        }
    }
}

val dependencyScoped = configurations.dependencyScope("dependencyScoped")
val resolvableClasspath = configurations.resolvable("resolvableClasspath") {
    attributes {
        attribute(a, "a")
        attribute(b, "b")
        attribute(c, "c1")
    }
    extendsFrom(dependencyScoped.get())
}

configurations {
    consumable("variant1") {
        attributes {
            attribute(a, "a")
            attribute(b, "b")
            attribute(c, "c1")
        }
        outgoing {
            capability("org.example:other:1.0.0")
            capability("org.example:example:1.0.0")
        }
    }
    consumable("variant2") {
        attributes {
            attribute(a, "a")
            attribute(b, "b")
            attribute(c, "c2")
        }
        outgoing {
            capability("org.example:example:1.0.0")
        }
    }
    consumable("variant3") {
        attributes {
            attribute(a, "a")
            attribute(c, "c1")
        }
        outgoing {
            capability("org.example:example:1.0.0")
        }
    }
}

dependencies {
    dependencyScoped(project(":")) {
        capabilities {
            requireCapability("org.example:example")
        }
    }
}

abstract class ResolveClasspath : DefaultTask() {
    @get:Input
    abstract val variantName: Property<String>

    @TaskAction
    fun execute() {
        println("Resolved to variant ${variantName.get()}")
    }
}

tasks.register<ResolveClasspath>("resolvesClasspath") {
    dependsOn(resolvableClasspath.get())
    variantName = resolvableClasspath.get().incoming.resolutionResult.rootComponent.map { it ->
        (it.dependencies.toList().get(0) as ResolvedDependencyResult).resolvedVariant.displayName
    }
}
