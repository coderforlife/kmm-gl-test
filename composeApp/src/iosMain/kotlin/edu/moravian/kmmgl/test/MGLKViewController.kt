@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
package edu.moravian.kmmgl.test

import angle.eglSwapInterval
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreFoundation.CFTimeInterval
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGSize
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

interface MGLViewListener {
    fun onLoad(controller: MGLKViewController) {}
    fun onUnload(controller: MGLKViewController) {}
    fun onPause(controller: MGLKViewController) {}
    fun onResume(controller: MGLKViewController) {}
    fun onResize(controller: MGLKViewController, width: Int, height: Int) {}
    fun onRender(controller: MGLKViewController, rect: Rect, timeSinceLastUpdate: Double) {}
}

class MGLKViewController(
    val context: MGLContext = MGLContext(3),
    val listener: MGLViewListener
) : UIViewController(null, null) {
    var glView = MGLKView(this)
    override fun loadView() { setView(glView) }
    override fun setView(view: UIView?) {
        if (view != glView) { throw IllegalArgumentException("MGLKViewController can only use the built-in MGLKView as its view") }
        super.setView(view)
    }

    // Setting to 0 or 1 will sync the frame rate with display's refresh rate
    var preferredFramesPerSecond = 30
        set(value) {
            if (value < 0) { throw IllegalArgumentException("preferredFramesPerSecond must be >= 0") }
            field = if (value == 1) 0 else value
            pause()
            resume()
        }
    var config get() = glView.glLayer.config
        set(value) {
            context.setConfig(value)
            glView.glLayer.config = value
        }

    ///// Basic Listener Events /////
    internal fun onRender(rect: CValue<CGRect>) { listener.onRender(this, rect.kRect, timeSinceLastUpdate) }
    private fun onResize() {
        val (width, height) = glView.glLayer.drawableSize
        listener.onResize(this, width, height)
    }
    override fun viewWillTransitionToSize(size: CValue<CGSize>, withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator)
        onResize()
    }
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        onResize()
    }
    override fun viewDidLoad() { listener.onLoad(this) }
    override fun viewDidUnload() { listener.onUnload(this) }


    ///// Frame Drawing /////
    private var timeSinceLastUpdate: CFTimeInterval = 0.0
    private var lastUpdateTime: CFTimeInterval = 0.0
    @Suppress("unused")
    @ObjCAction
    private fun draw() {
        val now = CACurrentMediaTime()

        if (appWasInBackground) {
            // To avoid time jump when the app goes to background for a long period of time
            lastUpdateTime = now
            appWasInBackground = false
            eglSwapInterval(context.display.egl, 0)
        }

        timeSinceLastUpdate = now - lastUpdateTime
        glView.display()
        lastUpdateTime = now
    }
    private var displayLink: CADisplayLink? = null
    private fun startDrawing() {
        val dl = displayLink ?: CADisplayLink.displayLinkWithTarget(this, selector=NSSelectorFromString("draw")).also { displayLink = it }
        dl.preferredFramesPerSecond = preferredFramesPerSecond.toLong()
        dl.addToRunLoop(NSRunLoop.mainRunLoop, forMode=NSRunLoopCommonModes) // TODO: or NSDefaultRunLoopMode?
    }
    private fun endDrawing() {
        displayLink?.removeFromRunLoop(NSRunLoop.mainRunLoop, forMode=NSRunLoopCommonModes) // TODO: or NSDefaultRunLoopMode?
        displayLink = null
    }


    ///// Pausing and Resuming /////
    private var _paused = true
    var paused get() = _paused
        set(value) { if (value) { this.pause() } else { this.resume() } }
    private fun pause() {
        if (_paused) { return }
        endDrawing()
        _paused = true
        listener.onPause(this)
    }
    private fun resume() {
        if (!_paused) { return }
        startDrawing()
        _paused = false
        onResize()
        listener.onResume(this)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        resume()
        NSNotificationCenter.defaultCenter.addObserver(
            this,
            NSSelectorFromString("appWillPause:"),
            "MGLKApplicationWillResignActiveNotification",
            null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            this,
            NSSelectorFromString("appDidBecomeActive:"),
            "MGLKApplicationDidBecomeActiveNotification",
            null
        )
    }
    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        appWasInBackground = true
        pause()
        NSNotificationCenter.defaultCenter.removeObserver(this, "MGLKApplicationWillResignActiveNotification", null)
        NSNotificationCenter.defaultCenter.removeObserver(this, "MGLKApplicationDidBecomeActiveNotification", null)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @ObjCAction
    private fun appWillPause(note: NSNotification) { appWasInBackground = true; pause() }
    @Suppress("unused", "UNUSED_PARAMETER")
    @ObjCAction
    private fun appDidBecomeActive(note: NSNotification) { resume() }

    private var appWasInBackground: Boolean = true
}
