package br.com.flaviotorres.iptvpremium.ui.components

fun main() {
    val router = CatalogEscapeRouter()
    var keyboard = false
    var categories = false
    var exits = 0
    var moves = 0
    val remove = router.register {
        when (catalogBackAction(keyboard, categories)) {
            CatalogBackAction.CLOSE_KEYBOARD -> keyboard = false
            CatalogBackAction.CATEGORIES -> { categories = true; moves++ }
            CatalogBackAction.EXIT -> exits++
        }
        true
    }
    fun press() { check(router.down(false)); check(router.up()) }
    press(); check(categories && moves == 1 && exits == 0)
    press(); check(exits == 1) // Exit on the second physical press, no disabled-handler handoff.
    categories = false; keyboard = true
    press(); check(!keyboard && !categories && exits == 1)
    press(); check(categories && exits == 1)
    check(router.down(false)); check(router.down(true)); check(router.down(true))
    check(exits == 2); check(router.up()); check(!router.up())
    remove(); check(!router.down(false)); check(!router.up())
    check(CatalogControlLook.PANEL_OPACITY == .90f)
    println("PASS grid -> categories -> exit in two physical presses; keyboard closes first; held BACK triggers once; handler removal; 90% background opacity")
}
