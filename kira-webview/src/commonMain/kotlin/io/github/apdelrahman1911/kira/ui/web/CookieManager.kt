package io.github.apdelrahman1911.kira.ui.web

/**
 * A cross-platform cookie manager for managing cookies in the WebView.
 *
 * Each platform wraps its native cookie management API:
 * - Android: `android.webkit.CookieManager`
 * - iOS: `WKHTTPCookieStore`
 * - Desktop: the existing JavaFX or application `CookieHandler`
 * - JS/WasmJS: No-op (not yet implemented)
 */
public expect class CookieManager() {
    /**
     * Sets a cookie for the given URL.
     * Desktop validates the cookie and requires an initialized or explicitly supplied handler.
     *
     * @param url The URL for which the cookie is to be set.
     * @param cookie The [Cookie] to set.
     */
    public suspend fun setCookie(url: String, cookie: Cookie)

    /**
     * Gets cookies for the given URL. On Desktop the handler selects applicable cookies,
     * and a generic handler may only provide their names and values.
     *
     * @param url The URL for which cookies are to be retrieved.
     * @return A list of [Cookie] objects associated with the URL.
     */
    public suspend fun getCookies(url: String): List<Cookie>

    /**
     * Removes all cookies where the store supports it. On Desktop a generic `CookieHandler`
     * throws [UnsupportedCookieOperationException] and remains untouched.
     */
    public suspend fun removeAllCookies()
}
