package app.debttracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DebtTest {
    private fun debt(period: InterestPeriod, rate: Double = 1.0) = Debt(
        1, "Ana\tPérez", Direction.I_OWE, 1000.0, "USD", rate, period, startDay = 100,
    )

    @Test fun dailyInterest() = assertEquals(100.0, debt(InterestPeriod.DAY).interest(110), 1e-9)

    @Test fun monthlyInterest() = assertEquals(20.0, debt(InterestPeriod.MONTH).interest(160), 1e-9)

    @Test fun noInterest() = assertEquals(1000.0, debt(InterestPeriod.NONE).total(500), 1e-9)

    @Test fun roundTrip() {
        val list = listOf(debt(InterestPeriod.DAY, 0.5), debt(InterestPeriod.MONTH).copy(id = 2, paid = true))
        assertEquals(list, parseDebts(serializeDebts(list)))
    }
}
