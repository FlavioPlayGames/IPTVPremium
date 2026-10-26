package br.com.flaviotorres.iptvpremium.ui.kids
fun main() {
    check(!KidsStartupPolicy.enabled(false,true))
    check(!KidsStartupPolicy.enabled(true,false))
    check(KidsStartupPolicy.enabled(true,true))
    for(route in listOf("home","movies","movie_details/{movieId}","playback/{id}","playlists","settings_fake")) {
        check(KidsStartupPolicy.redirect(true,true,false,route,false,false))
        check(!KidsStartupPolicy.redirect(true,true,true,route,false,false))
        check(!KidsStartupPolicy.redirect(false,true,false,route,false,false))
        check(!KidsStartupPolicy.redirect(true,false,false,route,false,false))
    }
    for(route in listOf("kids","settings","settings/subdl")) check(!KidsStartupPolicy.redirect(true,true,false,route,false,false))
    check(!KidsStartupPolicy.redirect(true,true,false,"movies",true,false))
    check(!KidsStartupPolicy.redirect(true,true,false,"movies",false,true))
    check(KidsStartupPolicy.protectSettings(true,true,false,false,true)) // every Kids shortcut still asks
    check(KidsStartupPolicy.protectSettings(true,false,false,false,false)) // missing PIN leads to creation, never verify=true
    check(!KidsStartupPolicy.protectSettings(false,false,true,true,false))
    check(KidsStartupPolicy.protectSettings(false,true,true,false,false))
    check(KidsStartupPolicy.protectSettings(false,true,false,true,false))
    check(!KidsStartupPolicy.protectSettings(false,true,true,true,true))
    println("PASS startup opt-in/parental requirement, restored-route isolation, settings-vs-child routes and existing normal mode")
    val permit=KidsSettingsPermit(10)
    check(!permit.approved)
    check(!permit.approve(false,true,10))
    check(!permit.approve(true,false,10)) // missing PIN store must not grant access
    check(!permit.approve(true,true,11)) // background/lock invalidates old callback
    check(permit.approve(true,true,10))
    permit.cancel();check(!permit.approved);check(!permit.approve(true,true,10))
    check(!KidsSettingsPermit(10).approved) // no persisted/reused permission on new entry
    val cancelled=KidsSettingsPermit(10);cancelled.cancel();check(!cancelled.approve(true,true,10))
    println("PASS incorrect/missing PIN denial, epoch check, cancelled verification and fresh entry authorization")
}
