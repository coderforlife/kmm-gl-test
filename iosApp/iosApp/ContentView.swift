import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
        //MyGLViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct SUIRenderView: UIViewRepresentable {
    typealias UIViewType = RenderView

    func makeUIView(context: Context) -> RenderView {
        // A zero size frame will cause window surface
        // creation failure
        return RenderView(frame: CGRect(origin: .zero, size: CGSize(width: 100, height: 100)))
    }

    func updateUIView(_ uiView: RenderView, context: Context) {}
}

struct ContentView: View {
    var body: some View {
//        SUIRenderView()
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
//
//let viewDelegate = ViewDelegate();
//
//class MyGLViewController: MGLKViewController {
//
//    var _glProgram : GLuint = 0
//
//    override func loadView() {
//        self.view = MGLKView();
//        self.glView?.delegate = viewDelegate;
//    }
//    
//    override func viewDidLoad() {
//        super.viewDidLoad()
//
//        let glContext = MGLContext(api: kMGLRenderingAPIOpenGLES3)
//        self.glView?.context = glContext
//
//        MGLContext.setCurrent(glContext)
//    }
//}
//
//
//class ViewDelegate: NSObject, MGLKViewDelegate {
//    //- (void)mglkView:(MGLKView *)view drawInRect:(CGRect)rect;
//
//    func mglkView(_ view: MGLKView, drawIn rect: CGRect) {
//        glClearColor(0.0, 1.0, 0.0, 1.0)
//        glClear(0x4000)
//    }
//}

//
//  RenderView.swift
//  ANGLESwiftUI
//
//  Created by Levin Li on 2023/6/9.
//

import libEGL
import libGLESv2

#if os(macOS)
import AppKit
#else
import UIKit
#endif

#if os(macOS)
enum DisplayLink {
    case cv(CVDisplayLink)
    case ca(NSObject)
}

typealias PlatformView = NSView
typealias PlatformDisplayLink = DisplayLink
#else
typealias PlatformView = UIView
typealias PlatformDisplayLink = CADisplayLink
#endif

class RenderView: PlatformView {
    private var display: EGLDisplay!
    private var surface: EGLSurface!
    private var context: EGLContext!
    private var program: GLuint!
    private var vao: GLuint!
    private var vbo: GLuint!

    private var displayLink: PlatformDisplayLink?

#if os(macOS)
    override func makeBackingLayer() -> CALayer {
        return CAMetalLayer()
    }
#else
    override class var layerClass: AnyClass {
        return CAMetalLayer.self
    }
#endif

    override init(frame: CGRect) {
        super.init(frame: frame)
        setUp()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setUp()
    }

#if !os(macOS)
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        guard previousTraitCollection?.displayScale != traitCollection.displayScale else {
            return
        }

        layer.contentsScale = traitCollection.displayScale
    }
#endif

    private func setUp() {
#if os(macOS)
        wantsLayer = true
#else
        layer.contentsScale = traitCollection.displayScale
#endif
        guard let display = eglGetPlatformDisplay(EGLenum(EGL_PLATFORM_ANGLE_ANGLE), nil, nil) else {
            print("eglGetPlatformDisplay() returned error \(eglGetError())")
            return
        }

        guard eglInitialize(display, nil, nil) != 0 else {
            print("eglInitialize() returned error \(eglGetError())")
            return
        }

        var configAttribs: [EGLint] = [
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_DEPTH_SIZE, 24,
            EGL_NONE,
        ]

        let configs = UnsafeMutablePointer<EGLConfig?>.allocate(capacity: 1)
        defer { configs.deallocate() }

        var numConfigs: EGLint = 0
        guard eglChooseConfig(display, &configAttribs, configs, 1, &numConfigs) != 0 else {
            print("eglChooseConfig() returned error \(eglGetError())")
            return
        }

        guard let config = configs.pointee else {
            print("Empty config returned in eglChooseConfig()")
            return
        }

        var contextAttribs: [EGLint] = [
            EGL_CONTEXT_MAJOR_VERSION, 2,
            EGL_CONTEXT_MINOR_VERSION, 0,
            EGL_NONE,
        ]

        guard let context = eglCreateContext(display, config, nil, &contextAttribs) else {
            print("eglCreateContext() returned error \(eglGetError())")
            return
        }

        guard let surface = eglCreateWindowSurface(display, config, unsafeBitCast(layer, to: EGLNativeWindowType.self), nil) else {
            print("eglCreateWindowSurface() returned error \(eglGetError())")
            return
        }

        self.surface = surface
        self.display = display
        self.context = context

        eglMakeCurrent(display, surface, surface, context)
        eglSwapInterval(display, 0)
        
        #if os(macOS)
        if #available(macOS 14.0, *) {
            let displayLink = self.displayLink(target: self, selector: #selector(displayLinkCallback))
            displayLink.add(to: .current, forMode: .common)
            self.displayLink = .ca(displayLink)
        } else {
            var displayLink: CVDisplayLink?
            CVDisplayLinkCreateWithActiveCGDisplays(&displayLink)
            CVDisplayLinkSetOutputCallback(displayLink!, { _, _, _, _, _, pointer in
                let view = unsafeBitCast(pointer, to: RenderView.self)
                DispatchQueue.main.async {
                    view.displayLinkCallback()
                }
                return kCVReturnSuccess
            }, UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque()))
            CVDisplayLinkStart(displayLink!)
            self.displayLink = .cv(displayLink!)
        }
        #else
        let displayLink = CADisplayLink(target: self, selector: #selector(displayLinkCallback))
        displayLink.add(to: .current, forMode: .common)
        self.displayLink = displayLink
        #endif
    }

    @objc private func displayLinkCallback() {
        eglMakeCurrent(display, surface, surface, context)

        let drawableSize = (self.layer as! CAMetalLayer).drawableSize
        glViewport(0, 0, GLsizei(drawableSize.width), GLsizei(drawableSize.height))

        glClearColor(0.0, 1.0, 0.0, 1.0)
        glClear(GLbitfield(GL_COLOR_BUFFER_BIT))

        eglSwapBuffers(display, surface)
    }
}
