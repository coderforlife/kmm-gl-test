package edu.moravian.kmmgl.test

import android.opengl.GLSurfaceView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.egl.EGLConfig

@Composable
actual fun GLView(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = GLSurfaceView(context).apply {
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onDrawFrame(gl: GL10?) {
                        gl?.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
                        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT)
                    }
                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {}
                })
            }
            FrameLayout(context).apply { addView(view) }
        }
    )
}
