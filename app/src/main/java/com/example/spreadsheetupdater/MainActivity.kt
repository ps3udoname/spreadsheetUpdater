package com.example.spreadsheetupdater

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spreadsheetupdater.ui.theme.SpreadsheetUpdaterTheme
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    private lateinit var startAuthorizationIntent: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiCaller = ApiCaller(this)

        startAuthorizationIntent =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                try {
                    // extract the result
                    val authorizationResult = Identity.getAuthorizationClient(this)
                        .getAuthorizationResultFromIntent(activityResult.data)
                    // continue with user action
                    apiCaller.updateSheet(authorizationResult)
                } catch (e: ApiException) {
                    // log exception
                    Log.e("MainaActivity", "Failed to authorize", e)
                }
            }

        Log.d("MainaActivity", "mainActivity run")

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            SpreadsheetUpdaterTheme {
                Greeting(apiCaller, startAuthorizationIntent)
            }
        }
    }
}

@Composable
fun Greeting(apiCaller: ApiCaller? = null, launcher: ActivityResultLauncher<IntentSenderRequest>? = null) {
    val focusManager = LocalFocusManager.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    var text1 by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var text2 by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var text3 by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var text4 by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }




    Column (
        modifier = Modifier
            .background(color = Color.Transparent)
            .fillMaxSize()
            // This clickable makes the background "tap to dismiss"
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Disables the ripple effect so the background feels like dead space
            ) {
                (context as? Activity)?.finish()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Without this, tapping on the labels or empty space between fields would also close the app.
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Consumes the click so it doesn't reach the background
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                value = text1,
                label = { Text("amount") },
                onValueChange = { text1 = it },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            TextField(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                value = text2,
                label = { Text("category") },
                onValueChange = { text2 = it },
                keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            TextField(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                value = text3,
                label = { Text("value") },
                onValueChange = { text3 = it },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            TextField(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                value = text4,
                label = { Text("context") },
                onValueChange = { text4 = it },
                keyboardOptions = KeyboardOptions(
                    // Changed to Search to show search icon on keyboard
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        
                        Log.d("MainaActivity", "Add clicked: ${text1.text}, ${text2.text}, ${text3.text}, ${text4.text}")
                        if (apiCaller != null && launcher != null) {
                            apiCaller.startAuthorization(launcher, listOf(currentDate, currentTime, text1.text, text2.text, text3.text, text4.text))
                        }
                        text1 = TextFieldValue("")
                        text2 = TextFieldValue("")
                        text3 = TextFieldValue("")
                        text4 = TextFieldValue("")

                        //dismiss keyboard
                        focusManager.clearFocus()
                        activity?.moveTaskToBack(true)
                    }
                )
            )
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    modifier = Modifier.alignBy(FirstBaseline),
                    onClick = {
                        Log.d("MainaActivity", "Settings clicked")
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "config")
                }

                Button(
                    modifier = Modifier.alignBy(FirstBaseline),
                    onClick = {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                        Log.d("MainaActivity", "Add clicked: ${text1.text}, ${text2.text}, ${text3.text}, ${text4.text}")
                        if (apiCaller != null && launcher != null) {
                            apiCaller.startAuthorization(launcher, listOf(currentDate, currentTime, text1.text, text2.text, text3.text, text4.text))
                        }
                        text1 = TextFieldValue("")
                        text2 = TextFieldValue("")
                        text3 = TextFieldValue("")
                        text4 = TextFieldValue("")
                    }
                ) {
                    Text("add")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SpreadsheetUpdaterTheme {
        Greeting()
    }
}
