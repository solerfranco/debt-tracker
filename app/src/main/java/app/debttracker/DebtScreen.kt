package app.debttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private fun todayEpochDay() = System.currentTimeMillis() / 86_400_000L

@Composable
fun DebtScreen(initial: List<Debt>, onChange: (List<Debt>) -> Unit) {
    var debts by remember { mutableStateOf(initial) }
    var adding by remember { mutableStateOf(false) }
    val today = todayEpochDay()

    fun update(new: List<Debt>) {
        debts = new
        onChange(new)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }) { Text("Nueva deuda") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Debt Tracker", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))
            Totals(debts.filter { !it.paid }, today)
            if (debts.isEmpty()) {
                Text("Todavía no cargaste deudas.", modifier = Modifier.padding(top = 24.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(debts, key = { it.id }) { d ->
                    DebtCard(
                        d, today,
                        onTogglePaid = { update(debts.map { x -> if (x.id == d.id) x.copy(paid = !x.paid) else x }) },
                        onDelete = { update(debts.filter { x -> x.id != d.id }) },
                    )
                }
                item { Row(Modifier.padding(bottom = 88.dp)) {} }
            }
        }
    }

    if (adding) {
        AddDebtDialog(
            onDismiss = { adding = false },
            onSave = { d ->
                update(listOf(d.copy(id = System.currentTimeMillis())) + debts)
                adding = false
            },
        )
    }
}

@Composable
private fun Totals(active: List<Debt>, today: Long) {
    // Totales por moneda: nunca se mezclan monedas distintas.
    val currencies = active.map { it.currency }.distinct()
    if (currencies.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            currencies.forEach { c ->
                val mine = active.filter { it.currency == c }
                val owe = mine.filter { it.direction == Direction.I_OWE }.sumOf { it.total(today) }
                val owed = mine.filter { it.direction == Direction.OWED_TO_ME }.sumOf { it.total(today) }
                Text("$c — Debo: ${formatMoney(owe, c)} · Me deben: ${formatMoney(owed, c)}")
            }
        }
    }
}

@Composable
private fun DebtCard(d: Debt, today: Long, onTogglePaid: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            val who = if (d.direction == Direction.I_OWE) "Le debo a ${d.person}" else "${d.person} me debe"
            Text(who + if (d.paid) " (saldada)" else "", style = MaterialTheme.typography.titleMedium)
            Text("Capital: ${formatMoney(d.principal, d.currency)}")
            if (d.period != InterestPeriod.NONE) {
                val per = if (d.period == InterestPeriod.DAY) "día" else "mes"
                Text("Interés: ${d.ratePercent}% por $per → ${formatMoney(d.interest(today), d.currency)}")
            }
            Text("Total: ${formatMoney(d.total(today), d.currency)}", style = MaterialTheme.typography.titleSmall)
            Row {
                TextButton(onClick = onTogglePaid) { Text(if (d.paid) "Reabrir" else "Marcar saldada") }
                TextButton(onClick = onDelete) { Text("Borrar") }
            }
        }
    }
}

@Composable
private fun AddDebtDialog(onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var person by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(Direction.I_OWE) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CURRENCIES.first()) }
    var rate by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(InterestPeriod.NONE) }
    var menu by remember { mutableStateOf(false) }

    val amountValue = amount.replace(',', '.').toDoubleOrNull()
    val rateValue = if (period == InterestPeriod.NONE) 0.0 else rate.replace(',', '.').toDoubleOrNull()
    val valid = person.isNotBlank() && amountValue != null && amountValue > 0 && rateValue != null && rateValue >= 0
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva deuda") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == Direction.I_OWE, { direction = Direction.I_OWE }, { Text("Yo debo") })
                    FilterChip(direction == Direction.OWED_TO_ME, { direction = Direction.OWED_TO_ME }, { Text("Me deben") })
                }
                OutlinedTextField(person, { person = it }, label = { Text("Persona") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        amount, { amount = it }, label = { Text("Monto") }, singleLine = true,
                        keyboardOptions = decimal, modifier = Modifier.weight(1f),
                    )
                    Column {
                        OutlinedButton(onClick = { menu = true }) { Text(currency) }
                        DropdownMenu(menu, { menu = false }) {
                            CURRENCIES.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; menu = false })
                            }
                        }
                    }
                }
                Text("Interés")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(period == InterestPeriod.NONE, { period = InterestPeriod.NONE }, { Text("Sin") })
                    FilterChip(period == InterestPeriod.DAY, { period = InterestPeriod.DAY }, { Text("Por día") })
                    FilterChip(period == InterestPeriod.MONTH, { period = InterestPeriod.MONTH }, { Text("Por mes") })
                }
                if (period != InterestPeriod.NONE) {
                    OutlinedTextField(
                        rate, { rate = it }, label = { Text("Tasa (%)") }, singleLine = true,
                        keyboardOptions = decimal,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        Debt(0, person.trim(), direction, amountValue!!, currency, rateValue!!, period, todayEpochDay()),
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
