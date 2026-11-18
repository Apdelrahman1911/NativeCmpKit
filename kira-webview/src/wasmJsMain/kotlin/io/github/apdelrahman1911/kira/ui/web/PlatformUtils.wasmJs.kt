package io.github.apdelrahman1911.kira.ui.web

import kotlin.js.ExperimentalWasmJsInterop
import org.w3c.dom.Window

@OptIn(ExperimentalWasmJsInterop::class)
private fun windowEval(window: Window, script: String): JsAny? =
    js("window.eval(script)")

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun Window.evaluateScript(script: String): Any? =
    windowEval(this, script)
