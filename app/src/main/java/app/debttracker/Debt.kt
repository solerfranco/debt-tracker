package app.debttracker

import java.util.Locale

enum class Direction { I_OWE, OWED_TO_ME }

enum class InterestPeriod { NONE, DAY, MONTH }

val CURRENCIES = listOf("ARS", "USD", "EUR", "BRL", "UYU", "CLP", "MXN", "COP", "GBP")

/** Deuda con interés simple. [startDay] es un día epoch (días desde 1970-01-01). */
data class Debt(
    val id: Long,
    val person: String,
    val direction: Direction,
    val principal: Double,
    val currency: String,
    val ratePercent: Double,
    val period: InterestPeriod,
    val startDay: Long,
    val paid: Boolean = false,
)

private const val DAYS_PER_MONTH = 30.0

/** Interés acumulado (simple) hasta [today]. Por mes se cuenta mes = 30 días. */
fun Debt.interest(today: Long): Double {
    val days = (today - startDay).coerceAtLeast(0)
    val periods = when (period) {
        InterestPeriod.NONE -> 0.0
        InterestPeriod.DAY -> days.toDouble()
        InterestPeriod.MONTH -> days / DAYS_PER_MONTH
    }
    return principal * ratePercent / 100.0 * periods
}

fun Debt.total(today: Long): Double = principal + interest(today)

fun formatMoney(amount: Double, currency: String): String =
    String.format(Locale.US, "%,.2f %s", amount, currency)

private fun esc(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

private fun unesc(s: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '\\' && i + 1 < s.length) {
            i++
            sb.append(
                when (s[i]) {
                    't' -> '\t'
                    'n' -> '\n'
                    else -> s[i]
                },
            )
        } else {
            sb.append(c)
        }
        i++
    }
    return sb.toString()
}

/** Una deuda por línea, campos separados por tab. */
fun serializeDebts(debts: List<Debt>): String = debts.joinToString("\n") {
    listOf(
        it.id, esc(it.person), it.direction.name, it.principal, esc(it.currency),
        it.ratePercent, it.period.name, it.startDay, it.paid,
    ).joinToString("\t")
}

fun parseDebts(text: String): List<Debt> = text.lines().filter { it.isNotBlank() }.mapNotNull { line ->
    runCatching {
        val f = line.split("\t")
        Debt(
            id = f[0].toLong(), person = unesc(f[1]), direction = Direction.valueOf(f[2]),
            principal = f[3].toDouble(), currency = unesc(f[4]), ratePercent = f[5].toDouble(),
            period = InterestPeriod.valueOf(f[6]), startDay = f[7].toLong(), paid = f[8].toBoolean(),
        )
    }.getOrNull()
}
