package com.fxf.adbhost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.tooling.preview.Preview
import com.fxf.adbhost.ui.theme.AdbHostTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = UsbController(this)
        controller.connect()

        setContent {
            AdbHostTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Greeting("Android")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = "退出HOST-APP",
                            modifier = Modifier.clickable {
                                finish()
                                System.exit(0)
                            },
                        )
                        Row {
                            var text by rememberSaveable { mutableStateOf("文字") }
                            TextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                },
                                /*modifier = Modifier.onKeyEvent {
                                    if (it.key == Key.Enter) {
                                        controller.send(text.toByteArray())
                                        return@onKeyEvent true
                                    }
                                    return@onKeyEvent false
                                },*/
                            )
                            Button(onClick = { controller.send(text.toByteArray()) }) {
                                Text(text = "send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AdbHostTheme {
        Greeting("Android")
    }
}
