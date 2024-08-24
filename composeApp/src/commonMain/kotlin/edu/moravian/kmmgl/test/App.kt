package edu.moravian.kmmgl.test

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        GLView(Modifier.fillMaxSize(0.5f))
    }
}

@Composable
expect fun GLView(modifier: Modifier = Modifier)
