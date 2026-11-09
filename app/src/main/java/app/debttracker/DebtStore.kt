package app.debttracker

import android.content.Context

class DebtStore(context: Context) {
    private val prefs = context.getSharedPreferences("debts", Context.MODE_PRIVATE)

    fun load(): List<Debt> = parseDebts(prefs.getString("data", "") ?: "")

    fun save(debts: List<Debt>) {
        prefs.edit().putString("data", serializeDebts(debts)).apply()
    }
}
