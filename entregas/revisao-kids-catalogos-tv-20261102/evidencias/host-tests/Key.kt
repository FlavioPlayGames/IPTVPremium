package android.view
class KeyEvent(val keyCode:Int, val action:Int, val repeatCount:Int=0) {
 companion object {const val KEYCODE_BACK=4; const val ACTION_DOWN=0;const val ACTION_UP=1}
}
