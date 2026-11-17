package com.mohamedrejeb.calf.ui.dialog

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.*
import com.mohamedrejeb.calf.core.*
import com.mohamedrejeb.calf.ui.dialog.uikit.*
import com.mohamedrejeb.calf.ui.utils.*
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName

/**
 * Manages the [UIAlertController] and its actions.
 *
 * @param onDismissRequest Lambda that is invoked when the dialog is dismissed.
 * @param iosProperties The iOS properties of the dialog.
 * @param properties The properties of the dialog.
 * @param parentUIViewController The [UIViewController] that will present the dialog.
 * @constructor Creates an [AlertDialogManager] instance.
 * @see [UIAlertController]
 */
@InternalCalfApi
class AlertDialogManager internal constructor(
    private val parentUIViewController: UIViewController,
    internal var onDismissRequest: () -> Unit,
    internal var iosProperties: AlertDialogIosProperties,
    internal var properties: DialogProperties,
    internal var layoutDirection: LayoutDirection = LayoutDirection.Ltr,
) : NSObject() {

    /**
     * Indicates whether the dialog is presented or not.
     */
    private var isPresented = false

    /**
     * Indicates whether the dialog is animating or not.
     */
    private var isAnimating = false

    /**
     * Strong references to text field delegate objects.
     * UIKit's addTarget holds targets weakly, so without these
     * the GC would collect the delegates and text change events
     * would stop firing.
     */
    private val textFieldDelegates = mutableListOf<NSObject>()

    /**
     * The ui view controller that is used to present the dialog.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private val dialogUIViewController: UIViewController by lazy {
        UIAlertController.alertControllerWithTitle(
            // An empty string still reserves a title/message row; nil omits it, as UIKit apps do.
            title = iosProperties.title.ifEmpty { null },
            message = iosProperties.text.ifEmpty { null },
            preferredStyle = iosProperties.style.toUIAlertControllerStyle()
        ).apply {
            iosProperties.actions.forEach { action ->
                val uiAlertAction = UIAlertAction.actionWithTitle(
                    title = action.title,
                    style = action.style.toUIAlertActionStyle(),
                    handler = {
                        // UIKit dismisses the controller itself before running an action handler.
                        // Record that, so disposing the dialog afterwards neither dismisses a
                        // controller that is no longer presented (which UIKit forwards to the
                        // presenting controller) nor reports a second, spurious dismiss request.
                        isPresented = false
                        isAnimating = false
                        action.onClick()
                    },
                )
                uiAlertAction.enabled = action.enabled
                addAction(uiAlertAction)

                if (action.isPreferred) {
                    setPreferredAction(uiAlertAction)
                }
            }

            if (iosProperties.style == AlertDialogIosStyle.Alert) {
                iosProperties.textFields.forEach { textFieldState ->
                    addTextFieldWithConfigurationHandler { textField ->
                        if (textField == null)
                            return@addTextFieldWithConfigurationHandler

                        val textChangedDelegate = object : NSObject() {
                            @ObjCAction
                            fun textFieldDidChange(sender: UITextField) {
                                textFieldState.onValueChange(sender.text.orEmpty())
                            }
                        }

                        // Keep a strong reference so GC doesn't collect the delegate
                        textFieldDelegates.add(textChangedDelegate)

                        textField.text = textFieldState.initialValue
                        textField.placeholder = textFieldState.placeholder
                        textField.keyboardType = textFieldState.keyboardType.toUIKeyboardType()
                        textField.setSecureTextEntry(textFieldState.isSecure)
                        textField.addTarget(
                            target = textChangedDelegate,
                            action = sel_registerName("textFieldDidChange:"),
                            forControlEvents = UIControlEventEditingChanged
                        )
                    }
                }
            }

            if (iosProperties.severity == AlertDialogIosSeverity.Critical)
                setSeverity(UIAlertControllerSeverityCritical)
        }
    }

    /**
     * Updates the properties of the already-presented [UIAlertController]
     * without dismissing and recreating it.
     *
     * Updatable properties:
     * - [UIAlertController.title] and [UIAlertController.message]
     * - [UIAlertAction.enabled] for each action
     */
    internal fun updateDialogProperties() {
        val controller = dialogUIViewController as? UIAlertController ?: return

        controller.title = iosProperties.title.ifEmpty { null }
        controller.message = iosProperties.text.ifEmpty { null }

        val actions = iosProperties.actions
        for (i in actions.indices) {
            if (i < controller.actions.size) {
                (controller.actions[i] as? UIAlertAction)?.enabled = actions[i].enabled
            }
        }
    }

    /**
     * Lambda that dismisses the dialog.
     */
    private val onDismissLambda: (() -> Unit) = {
        dialogUIViewController.dismissViewControllerAnimated(
            flag = true,
            completion = {
                isPresented = false
                isAnimating = false
                onDismissRequest()
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private val outsideTapPointer = sel_registerName("dismissOnOutsideTap:")

    /**
     * Dismisses the dialog for a tap on the dimmed area around it. The recognizer sits on the
     * alert's container, so it also sees taps on the alert itself; those belong to UIKit (an
     * action button or a text field) and must not dismiss the dialog without running the action.
     */
    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    @ObjCAction
    fun dismissOnOutsideTap(recognizer: UITapGestureRecognizer) {
        val alertView = dialogUIViewController.view
        val inside = recognizer.locationInView(alertView).useContents { alertView.bounds.useContents {
            x >= 0.0 && y >= 0.0 && x <= size.width && y <= size.height
        } }
        if (!inside) dismiss()
    }

    /**
     * Dismisses the dialog.
     */
    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun dismiss() {
        if (!isPresented || isAnimating) return
        isAnimating = true
        onDismissLambda.invoke()
    }

    /**
     * Shows the dialog.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun showAlertDialog() {
        if (isPresented || isAnimating) return
        isAnimating = true

        dialogUIViewController.applyLayoutDirection(layoutDirection)

        // On iPad an action sheet is a popover and UIKit raises an exception when it has no anchor:
        // centre it in the presenting view. iPhone must keep the system sheet (setting an anchor
        // there turns it into a centred popover without its Cancel button on iOS 26).
        val needsAnchor = iosProperties.style == AlertDialogIosStyle.ActionSheet &&
            UIDevice.currentDevice.userInterfaceIdiom != UIUserInterfaceIdiomPhone
        if (needsAnchor) dialogUIViewController.popoverPresentationController?.let { popover ->
            val anchor = parentUIViewController.view
            popover.sourceView = anchor
            popover.sourceRect = anchor.bounds.useContents {
                CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
            }
            popover.permittedArrowDirections = 0u
        }

        parentUIViewController.presentViewController(
            viewControllerToPresent = dialogUIViewController,
            animated = iosProperties.animated,
            completion = {
                isPresented = true
                isAnimating = false

                if (properties.dismissOnClickOutside) {
                    dialogUIViewController.view.superview?.setUserInteractionEnabled(true)
                    dialogUIViewController.view.superview?.addGestureRecognizer(
                        UITapGestureRecognizer(
                            target = this,
                            action = outsideTapPointer
                        ).apply {
                            // Never swallow touches meant for the alert's own buttons and fields.
                            cancelsTouchesInView = false
                        }
                    )
                }
            }
        )
    }

    companion object {
        fun AlertDialogIosActionStyle.toUIAlertActionStyle(): UIAlertActionStyle =
            when (this) {
                AlertDialogIosActionStyle.Default ->
                    UIAlertActionStyleDefault

                AlertDialogIosActionStyle.Cancel ->
                    UIAlertActionStyleCancel

                AlertDialogIosActionStyle.Destructive ->
                    UIAlertActionStyleDestructive
            }

        fun AlertDialogIosStyle.toUIAlertControllerStyle(): UIAlertControllerStyle =
            when (this) {
                AlertDialogIosStyle.ActionSheet ->
                    UIAlertControllerStyleActionSheet

                AlertDialogIosStyle.Alert ->
                    UIAlertControllerStyleAlert
            }

        fun AlertDialogIosAction.toUIAlertAction(): UIAlertAction {
            return UIAlertAction.actionWithTitle(
                title = title,
                style = style.toUIAlertActionStyle(),
                handler = {
                    onClick()
                }
            )
        }
    }
}