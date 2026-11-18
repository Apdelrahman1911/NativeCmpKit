import org.gradle.api.Project

/** Select JavaFX for the current desktop build and for runnable applications. */
fun Project.javaFxDependencies(): List<String> {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val x64 = arch in setOf("amd64", "x86_64")
    val arm64 = arch in setOf("aarch64", "arm64")
    val classifier = when {
        os.contains("linux") && x64 -> "linux"
        os.contains("linux") && arm64 -> "linux-aarch64"
        os.contains("mac") && x64 -> "mac"
        os.contains("mac") && arm64 -> "mac-aarch64"
        os.contains("windows") && x64 -> "win"
        else -> error("JavaFX desktop runtime is unsupported on $os/$arch")
    }
    return listOf("base", "graphics", "controls", "media", "web", "swing")
        .map { "org.openjfx:javafx-$it:19:$classifier" }
}
