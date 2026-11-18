import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skiko.wasm.onWasmReady
import kotlin.coroutines.resume

internal actual suspend fun awaitComposeRenderer() = suspendCancellableCoroutine { continuation ->
    onWasmReady { continuation.resume(Unit) }
}
