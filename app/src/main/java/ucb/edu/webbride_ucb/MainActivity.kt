package ucb.edu.webbride_ucb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ucb.edu.webbride_ucb.presentation.WebBridgeScreen
import ucb.edu.webbride_ucb.ui.theme.WebBridgeUCBTheme

/** Punto de entrada: la lógica está distribuida por responsabilidades. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WebBridgeUCBTheme { WebBridgeScreen() } }
    }
}
