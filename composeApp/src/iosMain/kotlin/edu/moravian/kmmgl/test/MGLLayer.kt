@file:OptIn(ExperimentalForeignApi::class)

package edu.moravian.kmmgl.test

import angle.EGLDisplay
import angle.EGLSurface
import angle.EGL_ALPHA_SIZE
import angle.EGL_BLUE_SIZE
import angle.EGL_BUFFER_PRESERVED
import angle.EGL_DEPTH_SIZE
import angle.EGL_DRAW
import angle.EGL_GL_COLORSPACE_KHR
import angle.EGL_GREEN_SIZE
import angle.EGL_NONE
import angle.EGL_NO_CONTEXT
import angle.EGL_NO_SURFACE
import angle.EGL_READ
import angle.EGL_RED_SIZE
import angle.EGL_SAMPLES
import angle.EGL_SAMPLE_BUFFERS
import angle.EGL_STENCIL_SIZE
import angle.EGL_SWAP_BEHAVIOR
import angle.eglDestroySurface
import angle.eglGetCurrentSurface
import angle.eglMakeCurrent
import angle.eglSurfaceAttrib
import angle.eglSwapBuffers
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSCoder
import platform.QuartzCore.CALayer
import platform.QuartzCore.CALayerMeta
import platform.QuartzCore.CAMetalLayer
import platform.QuartzCore.CATransaction
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner


@OptIn(BetaInteropApi::class)
class MGLLayer : CALayer {
    @OverrideInit // annotations needed for the UIView.layerClass handler to work correctly
    constructor() : super()
    @OverrideInit
    constructor(coder: NSCoder): super(coder)
    @OverrideInit
    constructor(layer: Any): super(layer)

    companion object : CALayerMeta() // needed for the UIView.layerClass handler

    private val _display = MGLDisplay.default
    private val _layer = CAMetalLayer().also {
        it.frame = this.bounds
        this.addSublayer(it)
    }

    override fun setContentsScale(contentsScale: CGFloat) {
        super.setContentsScale(contentsScale)
        _layer.contentsScale = contentsScale
    }

    // Size of the OpenGL framebuffer
    val drawableSize: Size get() {
        _layer.drawableSize.useContents { if (width == 0.0 && height == 0.0) { checkLayerSize() } }
        return _layer.drawableSize.kSize
    }

    var config = Config(); set(value) { field = value; releaseSurface() }

    // Present the contents of OpenGL backed framebuffer on screen as soon as possible.
    fun present(): Boolean {
        if (!eglSwapBuffers(_display.egl, eglSurface)) { return false }
        checkLayerSize()
        return true
    }

    private val _surface = EGLSurfaceHolder(_display.egl)
    @Suppress("unused")
    @OptIn(ExperimentalNativeApi::class)
    private val cleaner = createCleaner(_surface) { it.dispose() }

    val eglSurface: EGLSurface get() {
        if (_surface.isValid) { return _surface.surface }
        checkLayerSize()

        val surfaceAttribs = intArrayOf(
            EGL_RED_SIZE,       config.colorFormat.red,
            EGL_GREEN_SIZE,     config.colorFormat.green,
            EGL_BLUE_SIZE,      config.colorFormat.blue,
            EGL_ALPHA_SIZE,     config.colorFormat.alpha,
            EGL_DEPTH_SIZE,     config.depthFormat.value,
            EGL_STENCIL_SIZE,   config.stencilFormat.value,
            EGL_SAMPLE_BUFFERS, 0,
            EGL_SAMPLES,        config.multisample.value,
            EGL_NONE
        )
        val eglConfig = eglChooseConfig(_display.egl, surfaceAttribs)

        val creationAttribs = intArrayOf(EGL_GL_COLORSPACE_KHR, config.colorFormat.colorSpace, EGL_NONE)
        val surface = eglCreateWindowSurface(_display.egl, eglConfig, _layer, creationAttribs)
        _surface.surface = surface
        if (config.retainedBacking) {
            eglSurfaceAttrib(_display.egl, surface, EGL_SWAP_BEHAVIOR, EGL_BUFFER_PRESERVED)
        }
        return surface
    }
    private fun releaseSurface() { _surface.dispose() }

    private fun checkLayerSize() {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        _layer.frame = this.bounds
        _layer.bounds.useContents {
            _layer.drawableSize = CGSizeMake(
                size.width * _layer.contentsScale,
                size.height * _layer.contentsScale
            )
        }
        CATransaction.commit()
    }
}

private class EGLSurfaceHolder(private val display: EGLDisplay) {
    val isValid get() = _surface != EGL_NO_SURFACE
    var surface: EGLSurface
        get() {
            if (_surface == EGL_NO_SURFACE) { throw RuntimeException("EGLSurface disposed") }
            return _surface!!
        }
        set(value) {
            dispose()
            _surface = value
        }
    private var _surface: EGLSurface? = null
    fun dispose() {
        if (_surface != EGL_NO_SURFACE) {
            if (_surface == eglGetCurrentSurface(EGL_READ) || _surface == eglGetCurrentSurface(EGL_DRAW)) {
                eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
            }
            eglDestroySurface(display, _surface)
            _surface = EGL_NO_SURFACE
        }
    }
}
