
package com.moneyday

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val income: Boolean,
    val amount: Double,
    val note: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyDay(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyDay(context: Context) {

    val prefs = remember {
        context.getSharedPreferences("moneyday", Context.MODE_PRIVATE)
    }

    val transactions = remember {
        mutableStateListOf<Transaction>().apply {
            prefs.getString("records", "")!!
                .lines()
                .filter { it.isNotBlank() }
                .forEach { line ->
                    val p = line.split("|", limit = 3)
                    if (p.size == 3) {
                        val value = p[1].toDoubleOrNull()
                        if (value != null) {
                            add(Transaction(
                                income = p[0] == "true",
                                amount = value,
                                note = p[2]
                            ))
                        }
                    }
                }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var isIncome by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val balance = transactions.sumOf {
        if (it.income) it.amount else -it.amount
    }

    val incomeTotal = transactions.filter {
        it.income
    }.sumOf { it.amount }

    val expenseTotal = transactions.filter {
        !it.income
    }.sumOf { it.amount }

    fun save() {
        prefs.edit().putString(
            "records",
            transactions.joinToString("\n") {
                "${it.income}|${it.amount}|${it.note.replace("|", "/").replace("\n", " ")}"
            }
        ).apply()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MoneyDay 💰") }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true }
                ) {
                    Text("+")
                }
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Мій баланс")
                            Text(
                                "%.2f Kč".format(balance),
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Доходи")
                                Text("+ %.2f Kč".format(incomeTotal))
                            }
                        }

                        Card(Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Витрати")
                                Text("- %.2f Kč".format(expenseTotal))
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Історія операцій",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(transactions.reversed(), key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.note.ifBlank { "Операція" })
                                Text(if (item.income) "Дохід" else "Витрата")
                            }

                            Text(
                                (if (item.income) "+" else "-") +
                                    "%.2f Kč".format(item.amount)
                            )
                        }
                    }
                }

                item {
                    Text(
                        "MoneyDay — контроль твоїх фінансів",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Нова операція") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row {
                            FilterChip(
                                selected = isIncome,
                                onClick = { isIncome = true },
                                label = { Text("Дохід") }
                            )

                            Spacer(Modifier.width(8.dp))

                            FilterChip(
                                selected = !isIncome,
                                onClick = { isIncome = false },
                                label = { Text("Витрата") }
                            )
                        }

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Сума (Kč)") }
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Опис") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val value = amount.replace(",", ".").toDoubleOrNull()

                        if (value != null && value > 0) {
                            transactions.add(
                                Transaction(
                                    income = isIncome,
                                    amount = value,
                                    note = note
                                )
                            )

                            save()
                            amount = ""
                            note = ""
                            showDialog = false
                        }
                    }) {
                        Text("Зберегти")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                    }) {
                        Text("Скасувати")
                    }
                }
            )
        }
    }
}
