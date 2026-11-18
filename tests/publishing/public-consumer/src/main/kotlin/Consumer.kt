import io.github.apdelrahman1911.kira.io.KmpFile
import io.github.apdelrahman1911.kira.io.getName
import java.io.File

fun main() {
    check(KmpFile(File("nativecmpkit-consumer.txt")).getName() == "nativecmpkit-consumer.txt")
    println("Resolved and invoked Kira I/O from Maven Central.")
}
