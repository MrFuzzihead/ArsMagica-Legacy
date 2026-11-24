// Extra build configuration for the GTNH/RFG build.
//
// This file adds the verification harness:
//
//   * JUnit 4 + Mockito unit tests (src/test/java), run on a Java 8 toolchain because the mod targets Java 8
//     bytecode and Mockito 4.x is the newest Mockito that still runs there;
//   * JaCoCo line/branch reports plus a regression floor wired into `check`;
//   * a non-shipped in-game test mod (src/gameTest/java) and a `gameTestServer` task that boots a real dedicated
//     server, runs assertions against the real block/item registries, and exits with the result;
//   * a `gameTestClient` task (or `./gradlew runClient -PgameTest`) that runs the client half at the main menu.
//
// The layout and the reasoning mirror MrFuzzihead/SolarFlux-Legacy. The game test mod is only added to the
// classpath for `gameTestServer`, `gameTestClient` (or `-PgameTest`), so it never reaches the published jar.

import java.math.BigDecimal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Attaches the JaCoCo agent to a forked game.
 *
 * <p>
 * `Test` tasks get the agent from the jacoco plugin; a `JavaExec` run task needs it as an explicit `-javaagent`
 * argument. The agent jar is declared as a `@Classpath` input so Gradle resolves it with the task instead of at
 * configuration time (which keeps the configuration cache happy).
 * </p>
 */
abstract class JacocoAgentArgumentProvider : CommandLineArgumentProvider {

    @get:Classpath
    abstract val agent: ConfigurableFileCollection

    @get:Input
    abstract val destFile: Property<String>

    @get:Input
    abstract val includes: Property<String>

    @get:Input
    abstract val classDumpDir: Property<String>

    override fun asArguments(): Iterable<String> {
        val jar = extractAgent()
        return listOf(
            "-javaagent:" + jar.absolutePath +
                "=destfile=" + destFile.get() +
                ",append=false,dumponexit=true" +
                ",includes=" + includes.get() +
                ",classdumpdir=" + classDumpDir.get()
        )
    }

    /**
     * The `jacocoAgent` configuration resolves to the JaCoCo *distribution* jar, which has no `Premain-Class` of its
     * own: the real agent is `jacocoagent.jar` inside it. Gradle's own JaCoCo support unpacks it the same way.
     */
    private fun extractAgent(): java.io.File {
        val distribution = agent.files.single()
        val target = java.io.File(destFile.get()).parentFile.resolve("jacocoagent.jar")
        java.util.zip.ZipFile(distribution).use { zip ->
            val entry = zip.getEntry("jacocoagent.jar") ?: return distribution
            target.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
        return target
    }
}

// ---------------------------------------------------------------------------------------------------------
// Unit tests + coverage
// ---------------------------------------------------------------------------------------------------------

apply(plugin = "jacoco")

project.extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = "0.8.16"
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = true
    }
    // The mod is Java 8 bytecode. Running the suite on a Java 8 toolchain keeps the test JVM in the same generation
    // as the target and avoids Mockito/ByteBuddy surprises on Java 21+.
    javaLauncher.set(
        project.extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    )
    testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// The production classes implement compileOnly supertypes (cofh.api.energy, thaumcraft.api), and the
// Minecraft/Forge jars themselves arrive through `devOnlyNonPublishable`. A test cannot even name those classes
// unless both configurations are on the test classpath as well.
val mainSourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
listOf("testCompileClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name) {
        extendsFrom(configurations.getByName("compileOnly"))
        extendsFrom(configurations.getByName("devOnlyNonPublishable"))
    }
}

// Only the unit-test run feeds the default report. The in-game run writes build/jacoco/gameTest.exec (see the agent
// below), but a Forge 1.7.10 dev launch round-trips every class through the LaunchClassLoader, so the class ids in
// that file cannot be matched against the class files on disk. The `jacocoGameTestReport` task below uses the agent's
// class dump instead.
val jacocoExecFiles = project.files(project.layout.buildDirectory.file("jacoco/test.exec"))

fun jacocoClassDirs() = project.files(mainSourceSet.output.classesDirs).asFileTree.matching {
    // The generated version holder is empty; it cannot be covered.
    exclude("am2/Tags.class")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates HTML and XML JaCoCo coverage reports from the unit-test run"
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(jacocoExecFiles)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails the build when unit-test coverage drops below the configured regression floor"
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(jacocoExecFiles)

    violationRules {
        // Bundle-wide regression guard. The floor is the measured value minus a safety margin, not a target. The
        // suite currently covers the pure math/utility classes; the floor is deliberately low so the game tests can
        // carry the rest of the mod without failing the build.
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.002")
            }
        }
    }
}

// `check` runs the tests and enforces the floor, so CI catches coverage regressions.
tasks.named("check").configure {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
    dependsOn(tasks.named("jacocoTestReport"))
}

// ---------------------------------------------------------------------------------------------------------
// In-game test mod (never shipped)
// ---------------------------------------------------------------------------------------------------------

val gameTestSourceSet = project.extensions.getByType(SourceSetContainer::class.java).create("gameTest")

configurations.getByName(gameTestSourceSet.compileClasspathConfigurationName)
    .extendsFrom(configurations.getByName("compileClasspath"))
configurations.getByName(gameTestSourceSet.runtimeClasspathConfigurationName)
    .extendsFrom(configurations.getByName("runtimeClasspath"))
gameTestSourceSet.compileClasspath += mainSourceSet.output
gameTestSourceSet.compileClasspath += mainSourceSet.compileClasspath
gameTestSourceSet.runtimeClasspath += mainSourceSet.output
gameTestSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
// The game-test mod is plain Java 8. Leaving Jabel's compiler plugin on this source set's classpath makes javac
// auto-load it through ServiceLoader, which then needs ByteBuddy's agent (not present here).
gameTestSourceSet.compileClasspath = gameTestSourceSet.compileClasspath
    .filter { !it.name.contains("jabel") }

val isGameTestRun = project.hasProperty("gameTest") ||
    gradle.startParameter.taskNames.any { it.contains("gameTest") }

if (isGameTestRun) {
    // Only our package is instrumented. The file is dumped when the game exits, so a crashed or hung run simply
    // leaves no data behind. It is NOT merged into jacocoTestReport (see the note on jacocoGameTestReport).
    val jacocoProvider = objects.newInstance(JacocoAgentArgumentProvider::class.java)
    jacocoProvider.agent.from(configurations.named("jacocoAgent"))
    jacocoProvider.destFile.set(
        project.layout.buildDirectory.file("jacoco/gameTest.exec")
            .map { it.asFile.absolutePath }
    )
    jacocoProvider.includes.set("am2/*")
    jacocoProvider.classDumpDir.set(
        project.layout.buildDirectory.dir("jacoco/gameTestClasses")
            .map { it.asFile.absolutePath }
    )

    tasks.withType<JavaExec>().configureEach {
        if (name.startsWith("runServer") || name.startsWith("runClient")) {
            classpath += gameTestSourceSet.output
            // Read by the game-test mod. `fml.queryResult` answers FML's "a world references missing mods, continue
            // anyway?" prompt up front so CI never blocks on stdin.
            systemProperty("arsmagica2.gameTest", "true")
            systemProperty("fml.queryResult", "confirm")
            jvmArgumentProviders.add(jacocoProvider)
        }
    }
}

tasks.named("check").configure {
    dependsOn(gameTestSourceSet.classesTaskName)
}

// Deterministic run directory: accept the EULA, use a throwaway flat world and disable the vanilla watchdog.
// Everything lives under run/server (gitignored). This task runs before every gameTestServer invocation.
tasks.register("prepareGameTestServer") {
    group = "verification"
    description = "Prepares the server run directory with eula.txt and deterministic server.properties"

    val runDirectory = project.layout.projectDirectory.dir(
        (findProperty("runServerWorkingDirectory") as String?) ?: "run/server"
    )
    val eulaFile = runDirectory.file("eula.txt").asFile
    val propertiesFile = runDirectory.file("server.properties").asFile
    val worldDirectory = runDirectory.dir("gameTestWorld").asFile
    val classDumpDirectory = project.layout.buildDirectory.dir("jacoco/gameTestClasses").get().asFile

    doLast {
        // The JaCoCo class dump is append-ish by nature: a second run can leave a hashed duplicate of the same class
        // name, which JaCoCo refuses to analyze. Start every run with a clean dump.
        classDumpDirectory.deleteRecursively()
        eulaFile.parentFile.mkdirs()
        eulaFile.writeText("eula=true\n")
        propertiesFile.writeText(
            """
            # Written by ./gradlew gameTestServer - do not edit; the task rewrites this file.
            level-name=gameTestWorld
            level-type=FLAT
            online-mode=false
            allow-nether=false
            spawn-protection=0
            difficulty=0
            max-tick-time=-1
            view-distance=6
            motd=Ars Magica 2 game test
            """.trimIndent() + "\n"
        )
        worldDirectory.deleteRecursively()
    }
}

tasks.named("runServer").configure {
    // `org.gradle.parallel` is enabled. Without this, prepareGameTestServer and runServer can run at the same time,
    // deleting the world/class dump while the server is starting.
    mustRunAfter(tasks.named("prepareGameTestServer"))
}

tasks.register("gameTestServer") {
    group = "verification"
    description = "Boots a dedicated server, runs the in-game Ars Magica 2 tests, and exits with their result"
    dependsOn("prepareGameTestServer")
    dependsOn("runServer")
    dependsOn(gameTestSourceSet.classesTaskName)
}

tasks.register("gameTestClient") {
    group = "verification"
    description = "Launches the client, runs the client-side Ars Magica 2 tests at the main menu, and exits"
    dependsOn("runClient")
    dependsOn(gameTestSourceSet.classesTaskName)
}

// ---------------------------------------------------------------------------------------------------------
// In-game coverage report
// ---------------------------------------------------------------------------------------------------------
// A forked dev launch writes build/jacoco/gameTest.exec, but the LaunchClassLoader rewrites every class, so the
// on-disk class ids do not match. The agent is asked to dump the bytes it instrumented
// (`classdumpdir=build/jacoco/gameTestClasses`), and that dump is what this report reads. The task is not wired into
// `check`.

tasks.register<JacocoReport>("jacocoGameTestReport") {
    group = "verification"
    description = "Reports coverage from the in-game run using the classes the JaCoCo agent instrumented"

    val execFile = project.layout.buildDirectory.file("jacoco/gameTest.exec").get().asFile
    // No `onlyIf`: a script closure cannot be stored in the configuration cache. Empty execution data makes JaCoCo
    // skip the task, which is the same outcome.
    executionData.setFrom(project.files(execFile).filter { it.exists() })

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(project.layout.buildDirectory.file("reports/jacoco/gameTest/jacocoGameTestReport.xml"))
        html.outputLocation.set(project.layout.buildDirectory.dir("reports/jacoco/gameTest/html"))
    }

    classDirectories.setFrom(
        project.layout.buildDirectory.dir("jacoco/gameTestClasses").map { dir ->
            project.files(dir).asFileTree.matching {
                include("am2/**")
                exclude(
                    "am2/Tags.class",
                    // The game-test mod itself is not part of the mod.
                    "am2/gametest/**"
                )
            }
        }
    )
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
}

tasks.named("gameTestServer") {
    finalizedBy(tasks.named("jacocoGameTestReport"))
}
