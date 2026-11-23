package ucb.edu.webbride_ucb.domain.model

enum class BridgeAction(val key: String, val label: String, val destination: String) {
    OPEN_CAMERA("open_camera", "Abrir cámara", "android"),
    OPEN_MENU("open_menu", "Abrir menú Android", "android"),
    SHOW_TOAST("show_toast", "Mostrar aviso Android", "android"),
    TOGGLE_THEME("toggle_theme", "Cambiar tema web", "web"),
    OPEN_PANEL("open_panel", "Abrir panel web", "web"),
    SHOW_BANNER("show_banner", "Mostrar aviso web", "web")
}
