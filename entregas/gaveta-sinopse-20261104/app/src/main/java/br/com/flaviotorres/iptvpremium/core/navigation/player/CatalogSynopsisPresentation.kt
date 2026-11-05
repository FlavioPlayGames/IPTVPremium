package br.com.flaviotorres.iptvpremium.core.player

import java.util.Locale

internal data class CatalogSynopsisDetails(val plot:String?,val year:Int?=null,
    val genres:List<String> = emptyList(),val durationSeconds:Long?=null)
internal fun catalogCoverMetadata(year:Int?,rating:Double?):String {
    val note=rating?.takeIf{it.isFinite() && it>0 && it<=10}?.let{String.format(Locale.forLanguageTag("pt-BR"),"%.1f",it)}?:"—"
    return "${year?.toString()?:"Ano não informado"}  ★ $note"
}
internal fun catalogSynopsisMetadata(details:CatalogSynopsisDetails?,fallbackYear:Int?):String {
    val duration=details?.durationSeconds?.takeIf{it>0}?.let{seconds->
        val minutes=(seconds/60).coerceAtLeast(1)
        if(minutes>=60)"${minutes/60}h ${(minutes%60).toString().padStart(2,'0')}min"else "${minutes}min"
    }
    return listOfNotNull((details?.year?:fallbackYear)?.toString(),details?.genres?.filter{it.isNotBlank()}?.distinct()?.take(3)?.joinToString(", ")?.takeIf{it.isNotEmpty()},duration).joinToString(" • ")
}
internal const val SYNOPSIS_HANDLE_WIDTH=22f
internal const val SYNOPSIS_HANDLE_HEIGHT=84f
internal data class SynopsisDrawerGeometry(val left:Float,val top:Float,val height:Float,
    val fullWidth:Float,val visibleWidth:Float,val handleX:Float,val handleY:Float)
internal fun synopsisDrawerGeometry(viewportWidth:Float,viewportHeight:Float,panelWidth:Float,center:Float,progress:Float):SynopsisDrawerGeometry {
    val left=panelWidth.coerceIn(0f,(viewportWidth-SYNOPSIS_HANDLE_WIDTH).coerceAtLeast(0f))
    val width=(viewportWidth-left-SYNOPSIS_HANDLE_WIDTH).coerceIn(0f,420f)
    val height=minOf(360f,viewportHeight*.64f,(viewportHeight-32f).coerceAtLeast(0f))
    val top=(center-height/2).coerceIn(16f,(viewportHeight-height-16f).coerceAtLeast(16f))
    val visible=width*(if(progress.isFinite())progress.coerceIn(0f,1f)else 0f)
    val handleY=(center-SYNOPSIS_HANDLE_HEIGHT/2).coerceIn(8f,(viewportHeight-SYNOPSIS_HANDLE_HEIGHT-8f).coerceAtLeast(8f))
    return SynopsisDrawerGeometry(left,top,height,width,visible,left+visible,handleY)
}
