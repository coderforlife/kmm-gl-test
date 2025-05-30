@file:OptIn(ExperimentalForeignApi::class)

package edu.moravian.kmmgl.test

import angle.EGLContext
import angle.EGLDisplay
import angle.EGL_ALPHA_SIZE
import angle.EGL_BLUE_SIZE
import angle.EGL_CONTEXT_MAJOR_VERSION
import angle.EGL_CONTEXT_MINOR_VERSION
import angle.EGL_DEPTH_SIZE
import angle.EGL_DONT_CARE
import angle.EGL_DRAW
import angle.EGL_GREEN_SIZE
import angle.EGL_NONE
import angle.EGL_NO_CONTEXT
import angle.EGL_NO_SURFACE
import angle.EGL_READ
import angle.EGL_RED_SIZE
import angle.EGL_SAMPLES
import angle.EGL_SAMPLE_BUFFERS
import angle.EGL_STENCIL_SIZE
import angle.eglDestroyContext
import angle.eglGetCurrentContext
import angle.eglGetCurrentSurface
import angle.eglMakeCurrent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.ref.createCleaner


class MGLContext(val apiMajorVersion: Int = 3, val apiMinorVersion: Int = 0) {
    val display = MGLDisplay.default
    private val holder = EGLContextHolder(display.egl, apiMajorVersion, apiMinorVersion)
    @Suppress("unused")
    @OptIn(ExperimentalNativeApi::class)
    private val cleaner = createCleaner(holder) { it.dispose() }
    val isValid get() = holder.isValid
    val egl get() = holder.context
    fun setConfig(config: Config) {
        if (isValid && eglGetCurrentContext() == egl) { setCurrentContext() }
        holder.setConfig(apiMajorVersion, apiMinorVersion, config)
    }

    companion object {
        val currentContext: MGLContext? get() = tlsCurrentContext
        val currentLayer: MGLLayer? get() = tlsCurrentLayer
        fun setCurrentContext(context: MGLContext? = null, layer: MGLLayer? = null): Boolean {
            if (context == null) {
                tlsCurrentContext = null
                tlsCurrentLayer = null
                return eglMakeCurrent(MGLDisplay.default.egl, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT).bool
            }
            val layer = layer ?: tlsCurrentLayer
            val surface = layer?.eglSurface ?: EGL_NO_SURFACE
            val display = context.display.egl
            if ((eglGetCurrentContext() != context.egl ||
                 eglGetCurrentSurface(EGL_READ) != surface ||
                 eglGetCurrentSurface(EGL_DRAW) != surface) &&
                !eglMakeCurrent(display, surface, surface, context.egl)) { return false }
            tlsCurrentContext = context
            tlsCurrentLayer = layer
            return true
        }
    }
}

@ThreadLocal private var tlsCurrentContext: MGLContext? = null
@ThreadLocal private var tlsCurrentLayer: MGLLayer? = null

private val blankAttribs = intArrayOf(
    EGL_RED_SIZE,       EGL_DONT_CARE, EGL_GREEN_SIZE,   EGL_DONT_CARE,
    EGL_BLUE_SIZE,      EGL_DONT_CARE, EGL_ALPHA_SIZE,   EGL_DONT_CARE,
    EGL_DEPTH_SIZE,     EGL_DONT_CARE, EGL_STENCIL_SIZE, EGL_DONT_CARE,
    EGL_SAMPLE_BUFFERS, EGL_DONT_CARE, EGL_SAMPLES,      EGL_DONT_CARE,
    EGL_NONE
)

private class EGLContextHolder(private val display: EGLDisplay, major: Int, minor: Int) {
    val isValid get() = _context != EGL_NO_CONTEXT
    var context: EGLContext
        get() {
            if (_context == EGL_NO_SURFACE) { throw RuntimeException("EGLContext disposed") }
            return _context!!
        }
        set(value) {
            dispose()
            _context = value
        }
    private var _context: EGLContext? = init(major, minor)
    private fun init(major: Int, minor: Int, config: Config? = null): EGLContext {
        val eglConfig = if (config == null) eglChooseConfig(display, blankAttribs)
                        else eglChooseConfig(display, config)

        val ctxAttribs = intArrayOf(
            EGL_CONTEXT_MAJOR_VERSION, major,
            EGL_CONTEXT_MINOR_VERSION, minor,
            EGL_NONE
        )
        return eglCreateContext(display, eglConfig, EGL_NO_CONTEXT, ctxAttribs)
    }
    fun setConfig(major: Int, minor: Int, config: Config) {
        dispose()
        _context = init(major, minor, config)
    }
    fun dispose() {
        if (_context != EGL_NO_CONTEXT) {
            if (eglGetCurrentContext() == context) {
                eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
            }
            eglDestroyContext(display, _context)
            _context = EGL_NO_CONTEXT
        }
    }
}
