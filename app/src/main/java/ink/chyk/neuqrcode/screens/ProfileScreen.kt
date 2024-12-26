package ink.chyk.neuqrcode.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.*

@Composable
fun ProfileScreen(navController: NavController) {
    var text by remember { mutableStateOf("Hello, World!") }

    Text(text)
}