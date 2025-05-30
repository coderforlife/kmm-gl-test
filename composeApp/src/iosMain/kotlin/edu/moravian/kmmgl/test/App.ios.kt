package edu.moravian.kmmgl.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import angle.GL_COLOR_BUFFER_BIT
import angle.glClear
import angle.glClearColor
import kotlinx.cinterop.ExperimentalForeignApi

@ExperimentalForeignApi
@Composable
actual fun GLView(modifier: Modifier) {
    UIKitViewController(
        modifier = modifier,
        factory = {
            MGLKViewController(MGLContext(3), ViewListener()).apply {
                config = Config(
                    colorFormat = ColorFormat.RGBA8888,
                    depthFormat = DepthFormat.DF16,
                    stencilFormat = StencilFormat.None,
                    multisample = Multisample.X4,
                )
            }
        },
    )
}

@ExperimentalForeignApi
private class ViewListener: MGLViewListener {
    override fun onLoad(controller: MGLKViewController) {
        println("onLoad")
    }

    override fun onUnload(controller: MGLKViewController) {
        println("onUnload")
    }

    override fun onPause(controller: MGLKViewController) {
        println("onPause")
    }

    override fun onResume(controller: MGLKViewController) {
        println("onResume")
    }

    override fun onResize(controller: MGLKViewController, width: Int, height: Int) {
        println("onResize: $width x $height")
    }

    var frame = 0
    override fun onRender(controller: MGLKViewController, rect: Rect, timeSinceLastUpdate: Double) {
        println("onRender: ${++frame}, $timeSinceLastUpdate seconds since last update")
        glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT.toUInt())
    }
}
