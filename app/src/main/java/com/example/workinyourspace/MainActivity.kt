package com.example.workinyourspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import com.example.workinyourspace.presentation.WorkspaceScreen
import com.example.workinyourspace.presentation.WorkspaceViewModel
import com.example.workinyourspace.ui.theme.WorkInYourSpaceTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WorkspaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkInYourSpaceTheme {
                WorkspaceScreen(viewModel = viewModel)
            }
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkInYourSpaceTheme {
        Greeting("Android")
    }
}