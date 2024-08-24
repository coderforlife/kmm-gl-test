package edu.moravian.kmmgl.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRect
import platform.EAGL.EAGLContext
import platform.Foundation.NSBundle
import platform.GLKit.GLKView
import platform.GLKit.GLKViewController
import platform.GLKit.GLKViewControllerDelegateProtocol
import platform.GLKit.GLKViewDelegateProtocol
import platform.darwin.NSObject
import platform.gles.glClear
import platform.gles.glClearColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GLView(modifier: Modifier) {
    // If we don't wait for a bit, the program will usually crash after the first frame is rendered
    // Every so often it will work fine, but it's not reliable
    // Even when waiting, it will crash after exactly 300 frames (10 seconds)
    var display by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        println("waiting")
        delay(3000)
        println("done waiting")
        display = true
    }
    if (!display) { return }

    UIKitViewController(
        modifier = modifier,
        factory = {
            ViewController().apply {
                delegate = ViewControllerDelegate()
            }
        },
    )
}

private class ViewController(nibName: String? = null, bundle: NSBundle? = null): GLKViewController(nibName, bundle) {
    override fun loadView() {
        setView(GLKView().apply {
            context = EAGLContext(3uL) // also tried with 2uL
//            drawableColorFormat = 0
//            drawableDepthFormat = 1
//            drawableStencilFormat = 0
//            drawableMultisample = 1
            delegate = ViewDelegate()
        })
    }

    override fun viewDidUnload() {
        super.viewDidUnload()
        setView(null)
    }
}

private class ViewControllerDelegate: NSObject(), GLKViewControllerDelegateProtocol {
    override fun glkViewControllerUpdate(controller: GLKViewController) {
        println("glkViewControllerUpdate")
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ViewDelegate: NSObject(), GLKViewDelegateProtocol {
    var frame = 0
    override fun glkView(view: GLKView, drawInRect: CValue<CGRect>) {
        println("glkView: ${++frame}")
        glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        glClear(0x4000u)
    }
}
