package io.github.apdelrahman1911.kira.ui

@RequiresOptIn(
    "This Kira UI API is experimental and is likely to change or to be removed in" +
            " the future.",
    level = RequiresOptIn.Level.WARNING
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalKiraUiApi()
