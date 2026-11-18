package io.github.apdelrahman1911.kira.ui.web

import org.w3c.dom.Window

internal actual fun Window.evaluateScript(script: String): Any? =
    asDynamic().eval(script)
