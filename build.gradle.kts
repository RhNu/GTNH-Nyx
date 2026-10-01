plugins {
    id("com.github.ElytraServers.elytra-conventions") version "v1.1.2.3"
    id("com.gtnewhorizons.gtnhconvention")
}

val gtnhManifestVersions = elytraModpackVersion.getModVersions().toMap()
val gtnhManifestVersion = providers.gradleProperty("elytra.manifest.version").get()
// IC2 2.2.828 in RC1: https://www.curseforge.com/minecraft/mc-mods/industrial-craft/files/6833054
val ic2CurseFileId = "6833054"
val modpackArtifactVersions = gtnhManifestVersions.mapKeys { (name, _) -> "com.github.GTNewHorizons:$name" } + mapOf(
    "com.github.GTNewHorizons:Mobs-Info" to "${gtnhManifestVersions.getValue("Mobs Info")}-GTNH",
    "com.falsepattern:chunkapi-mc1.7.10" to gtnhManifestVersions.getValue("Chunk API"),
    "com.falsepattern:endlessids-mc1.7.10" to gtnhManifestVersions.getValue("EndlessIDs"),
    "com.falsepattern:falsepatternlib-mc1.7.10" to gtnhManifestVersions.getValue("FalsepatternLib"),
    "ganymedes01.etfuturum:Et-Futurum-Requiem" to gtnhManifestVersions.getValue("Et-Futurum-Requiem"),
    "io.github.legacymoddingmc:unimixins" to gtnhManifestVersions.getValue("UniMixins"),
    // Modrinth versions omit the distribution filename's fairplay suffix.
    "maven.modrinth:journeymap" to gtnhManifestVersions.getValue("JourneyMap").removeSuffix("-fairplay"),
    "curse.maven:ic2-242638" to ic2CurseFileId,
)

configurations.configureEach {
    val artifactVersions = modpackArtifactVersions
    val alignmentReason = "Align with GTNH $gtnhManifestVersion"
    resolutionStrategy {
        exclude(group = "com.github.GTNewHorizons", module = "CodeChickenLib")
        exclude(group = "net.glease", module = "tc4recipelib")
        // Mod POMs can lag behind the pack; compile and test against its transitive versions too.
        eachDependency {
            // Inspect the target so GTNHGradle's IC2-to-Curse substitution is aligned as well.
            val manifestVersion = artifactVersions["${target.group}:${target.name}"]
            if (manifestVersion != null) {
                useVersion(manifestVersion)
                because(alignmentReason)
            }
        }
    }
}

dependencies {
    // RFG matches exact coordinates; the convention plugin only registers its older IC2 fallback.
    rfg.deobf("curse.maven:ic2-242638:$ic2CurseFileId")
    testImplementation(kotlin("test"))
    // Match GT5U's unit-test runtime for OverclockCalculator / GTUtility.
    testRuntimeOnly("org.joml:joml:1.10.8")
    testRuntimeOnly("it.unimi.dsi:fastutil:8.5.18")
    testRuntimeOnly("xyz.wagyourtail.jvmdowngrader:jvmdowngrader-java-api:1.3.5:downgraded-8") {
        isTransitive = false
    }
}
