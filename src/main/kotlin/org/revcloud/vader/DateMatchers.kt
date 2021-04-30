@file:JvmName("DateMatchers")

package org.revcloud.vader

import io.vavr.Function2
import java.util.*

val isOnOrBefore = Function2 { date1: Any?, date2: Any? ->
    when {
        date1 == null && date2 == null -> true
        date1 == null && date2 != null -> false
        date1 !is Date || date2 !is Date -> false
        else -> date1 == date2 || date1.before(date2)
    }
}

val isBefore = Function2 { date1: Any?, date2: Any? ->
    when {
        date1 == null && date2 == null -> true
        date1 == null && date2 != null -> false
        date1 !is Date || date2 !is Date -> false
        else -> date1.before(date2)
    }
}

val isEqualToDayOfDate = Function2 { day: Any?, date: Any? ->
    when {
        day == null && date == null -> false
        day != null && date == null -> false
        day !is Int || date !is Date -> false
        else -> day == date.date
    }
}
