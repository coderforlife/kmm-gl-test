@file:OptIn(ExperimentalForeignApi::class)

package edu.moravian.kmmgl.test

import angle.EGLDisplay
import angle.EGL_DEFAULT_DISPLAY
import angle.eglGetDisplay
import angle.eglTerminate
import angle.eglInitialize
import angle.EGL_NO_DISPLAY
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

class MGLDisplay(id: Int) {
    private val holder: EGLDisplayHolder = EGLDisplayHolder(id)
    @Suppress("unused")
    @OptIn(ExperimentalNativeApi::class)
    private val cleaner = createCleaner(holder) { it.dispose() }
    val isValid get() = holder.isValid
    val egl get() = holder.display
    companion object {
        val default by lazy { MGLDisplay(EGL_DEFAULT_DISPLAY) }
    }
}

private class EGLDisplayHolder(id: Int) {
    val isValid get() = _display != EGL_NO_DISPLAY
    val display: EGLDisplay get() {
        if (_display == EGL_NO_DISPLAY) { throw RuntimeException("EGLDisplay disposed") }
        return _display!!
    }
    private var _display: EGLDisplay? = init(id)
    private fun init(id: Int): EGLDisplay {
        val display = check(eglGetDisplay(id), EGL_NO_DISPLAY, "eglGetDisplay")
        if (!eglInitialize(display, null, null)) { throw RuntimeException("eglInitialize() failed") }
        return display
    }

    fun dispose() {
        if (_display != EGL_NO_DISPLAY) {
            eglTerminate(_display)
            _display = EGL_NO_DISPLAY
        }
    }
}
