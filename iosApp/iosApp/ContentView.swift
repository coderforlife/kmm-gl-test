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

struct ContentView: View {
    var body: some View {
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
