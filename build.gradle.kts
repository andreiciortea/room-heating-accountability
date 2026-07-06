plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

group = "org.hyperagents.demo"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://raw.github.com/jacamo-lang/mvn-repo/master")
    }
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases/")  // For current version of Gradle tooling API
    }
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases-local/") // For older versions of Gradle tooling API
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.jacamo:jacamo:1.2.2")
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir("src/main/jade")
            srcDir("src/main/cartago")
        }
        resources {
            srcDir("src/main/resources")
            srcDir("src/main/jason")
            srcDir("src/main/moise")
        }
    }
    test {
        java {
            srcDir("test/main/java")
            srcDir("test/main/jade")
            srcDir("test/main/cartago")
        }
        resources {
            srcDir("test/main/resources")
            srcDir("test/main/jason")
            srcDir("test/main/moise")
        }
    }
}

tasks {
    register<JavaExec>("runAgents") {
        description = "Runs the JaCaMo application launching the agents"
        dependsOn("classes")
        mainClass = "jacamo.infra.JaCaMoLauncher"
        args = listOf("room_heating.jcm")
        classpath = sourceSets.main.get().runtimeClasspath
    }
}

tasks {
    register<JavaExec>("runAgentsHuman") {
        description = "Runs the JaCaMo application launching the agents"
        dependsOn("classes")
        mainClass = "jacamo.infra.JaCaMoLauncher"
        args = listOf("room_heating_human.jcm")
        classpath = sourceSets.main.get().runtimeClasspath
    }
}

fun resolveSkillPath(project: Project): String {
    val skillProp = project.findProperty("skill") as? String
    if (skillProp != null) {
        if (!project.file(skillProp).exists()) error("Skill file not found: $skillProp")
        return skillProp
    }
    val runProp = project.findProperty("run") as? String
    val scenarioProp = project.findProperty("scenario") as? String
    if (runProp != null && scenarioProp != null) {
        val model = project.findProperty("model") as? String ?: "claude-opus-4-5"
        val path = "logs/$model/$scenarioProp/temp-management-run-$runProp.asl"
        if (!project.file(path).exists()) error("Skill file not found: $path (resolved from --scenario $scenarioProp --run $runProp)")
        return path
    }
    return ""
}

tasks {
    register<JavaExec>("replayAgents") {
        description = "Replays a base-scenario experiment in replay_corrective or replay_preventive mode"
        dependsOn("classes")
        mainClass = "jacamo.infra.JaCaMoLauncher"
        args = listOf("room_heating.jcm")
        classpath = sourceSets.main.get().runtimeClasspath
        val mode = project.findProperty("mode") as? String ?: "experiment"
        val skillPath = resolveSkillPath(project)
        systemProperty("RUN_MODE", mode)
        systemProperty("PATCHED_SKILL_PATH", skillPath)
    }
}

tasks {
    register<JavaExec>("replayAgentsHuman") {
        description = "Replays a human-scenario experiment in replay_corrective or replay_preventive mode"
        dependsOn("classes")
        mainClass = "jacamo.infra.JaCaMoLauncher"
        args = listOf("room_heating_human.jcm")
        classpath = sourceSets.main.get().runtimeClasspath
        val mode = project.findProperty("mode") as? String ?: "experiment"
        val skillPath = resolveSkillPath(project)
        systemProperty("RUN_MODE", mode)
        systemProperty("PATCHED_SKILL_PATH", skillPath)
    }
}