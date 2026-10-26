package br.com.flaviotorres.iptvpremium.ui.settings
internal object PinDigits {
    const val maximum = 6
    fun normalize(value: String) = value.filter { it in '0'..'9' }.take(maximum)
    fun append(value: String, digit: Int) = if (digit in 0..9 && value.length < maximum) value + digit else value
    fun erase(value: String) = value.dropLast(1)
    fun ready(value: String) = value.length == maximum && value.all { it in '0'..'9' }
}
