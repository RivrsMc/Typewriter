repositories {}
dependencies {
    compileOnly(project(":RoadNetworkExtension"))
    compileOnly(project(":QuestExtension"))
    testImplementation(kotlin("test"))
    testImplementation("com.typewritermc:engine-core:0.9.0")
    testImplementation(project(":RoadNetworkExtension"))
}

tasks.test {
    useJUnitPlatform()
}

typewriter {
    namespace = "typewritermc"

    extension {
        name = "Entity"
        shortDescription = "Create custom entities."
        description = """
            |The Entity Extension contains all the essential entries working with entities.
            |It allows you to create dynamic entities such as NPC's or Holograms.
            |
            |In most cases, it should be installed with Typewriter.
            |If you haven't installed Typewriter or the extension yet,
            |please follow the [Installation Guide](https://docs.typewritermc.com/docs/getting-started/installation)
            |first.
        """.trimMargin()
        engineVersion = "0.9.0-beta-170"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.NONE

        dependencies {
            dependency("typewritermc", "RoadNetwork")
            dependency("typewritermc", "Quest")
        }

        paper()
    }
}
