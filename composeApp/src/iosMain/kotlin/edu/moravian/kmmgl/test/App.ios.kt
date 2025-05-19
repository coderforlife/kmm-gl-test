package edu.moravian.kmmgl.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRect
import platform.Foundation.NSBundle
import platform.darwin.NSObject
import platform.gles.glClear
import platform.gles.glClearColor

//import platform.EAGL.EAGLContext as GLContext
//import platform.GLKit.GLKView
//import platform.GLKit.GLKViewController
//import platform.GLKit.GLKViewControllerDelegateProtocol
//import platform.GLKit.GLKViewDelegateProtocol

import metal_angle.MGLContext as GLContext
import metal_angle.MGLKView as GLKView
import metal_angle.MGLKViewController as GLKViewController
import metal_angle.MGLKViewControllerDelegateProtocol as GLKViewControllerDelegateProtocol
import metal_angle.MGLKViewDelegateProtocol as GLKViewDelegateProtocol

@ExperimentalForeignApi
@Composable
actual fun GLView(modifier: Modifier) {
    UIKitViewController(
        modifier = modifier,
        factory = {
            ViewController().apply {
                delegate = controllerDelegateHolder
            }
        },
    )
}

@ExperimentalForeignApi
private val controllerDelegateHolder = ViewControllerDelegate()

@ExperimentalForeignApi
private val delegateHolder = ViewDelegate()

@ExperimentalForeignApi
private class ViewController(nibName: String? = null, bundle: NSBundle? = null): GLKViewController(nibName, bundle) {
    override fun loadView() {
        //setView(GLKView().apply {
        setView(GLKView().apply {
            context = GLContext(3) // uL
//            drawableColorFormat = 0
//            drawableDepthFormat = 1
//            drawableStencilFormat = 0
//            drawableMultisample = 1
            delegate = delegateHolder
        })
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        (this.view as? GLKView)?.context?.let {
            GLContext.setCurrentContext(it)
        }
    }

    override fun viewDidUnload() {
        super.viewDidUnload()
        setView(null)
    }
}

@ExperimentalForeignApi
private class ViewControllerDelegate: NSObject(), GLKViewControllerDelegateProtocol {
    override fun mglkViewControllerUpdate(controller: GLKViewController) {
        //println("glkViewControllerUpdate")
    }
}

@ExperimentalForeignApi
private class ViewDelegate: NSObject(), GLKViewDelegateProtocol {
    //var frame = 0
    override fun mglkView(view: GLKView?, drawInRect: CValue<CGRect>) {
        //println("glkView: ${++frame}")
        glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        glClear(0x4000u)
    }
}
