// Modified for NativeCmpKit migration (2026-10-24): sample identity only.
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.apdelrahman1911.kira.picker.ProvideFilePickerParentWindow
import io.github.apdelrahman1911.kira.sample.App


fun main() {
    System.setProperty("compose.interop.blending", "true")

    application {
        Window(
            title = "NativeCmpKit",
            onCloseRequest = ::exitApplication
        ) {
            ProvideFilePickerParentWindow {
                App()
            }
        }
    }
}
