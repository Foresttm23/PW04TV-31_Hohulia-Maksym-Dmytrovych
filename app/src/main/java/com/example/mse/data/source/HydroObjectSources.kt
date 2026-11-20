package com.example.mse.data.source

import com.example.mse.model.HydroObject
import com.example.mse.model.HydroObjectType.HPP
import com.example.mse.model.HydroObjectType.PSPP
import com.example.mse.model.HydroObjectType.SMALL_HPP
import com.example.mse.model.OperatingStatus.MAINTENANCE
import com.example.mse.model.OperatingStatus.OFFLINE
import com.example.mse.model.OperatingStatus.REDUCED
import com.example.mse.model.OperatingStatus.RUNNING
import kotlinx.coroutines.delay

interface HydroObjectSource {
    suspend fun load(): List<HydroObject>
}

// Тестове джерело: координати та потужності орієнтовні (навчальні дані), час оновлення рахується від "зараз".
class MockHydroObjectSource(
    private val latencyMs: Long = 600,
    private val clock: () -> Long = System::currentTimeMillis
) : HydroObjectSource {
    override suspend fun load(): List<HydroObject> {
        delay(latencyMs)
        val now = clock()
        fun ago(minutes: Int) = now - minutes * 60_000L
        return listOf(
            HydroObject("kyiv-hpp", "Київська ГЕС", HPP, "Дніпро", "Вишгород", 50.585, 30.483, 408.5, 312.0, RUNNING,
                "Перша станція Дніпровського каскаду, працює у піковому режимі.", ago(4)),
            HydroObject("kyiv-pspp", "Київська ГАЕС", PSPP, "Дніпро", "Вишгород", 50.627, 30.388, 235.5, 160.0, RUNNING,
                "Акумулює енергію вночі, віддає в пікові години ранку та вечора.", ago(11)),
            HydroObject("kaniv-hpp", "Канівська ГЕС", HPP, "Дніпро", "Канів", 49.762, 31.495, 444.0, 380.0, RUNNING,
                "Водосховище комплексного призначення, судноплавний шлюз.", ago(7)),
            HydroObject("kremenchuk-hpp", "Кременчуцька ГЕС", HPP, "Дніпро", "Світловодськ", 49.068, 33.255, 625.6, 410.0, RUNNING,
                "Найбільше за площею водосховище України, сезонне регулювання стоку.", ago(2)),
            HydroObject("seredniodniprovska-hpp", "Середньодніпровська ГЕС", HPP, "Дніпро", "Кам'янське", 48.522, 34.605, 352.0, 90.0, REDUCED,
                "Знижене навантаження через обмеження припливу.", ago(35)),
            HydroObject("dnipro-hpp", "Дніпровська ГЕС", HPP, "Дніпро", "Запоріжжя", 47.870, 35.087, 1569.0, 1100.0, RUNNING,
                "Найпотужніша ГЕС України, регулює нижню ділянку каскаду.", ago(15)),
            HydroObject("kakhovka-hpp", "Каховська ГЕС", HPP, "Дніпро", "Нова Каховка", 46.780, 33.368, 334.8, 0.0, OFFLINE,
                "Зруйнована у червні 2023 року, виробництво відсутнє.", ago(60 * 24 * 30)),
            HydroObject("dniester-hpp", "Дністровська ГЕС", HPP, "Дністер", "Новодністровськ", 48.545, 27.475, 702.0, 540.0, RUNNING,
                "Регулює стік Дністра, захищає від паводків нижню течію.", ago(9)),
            HydroObject("dniester-pspp", "Дністровська ГАЕС", PSPP, "Дністер", "Новодністровськ", 48.560, 27.440, 972.0, 486.0, RUNNING,
                "Одна з найпотужніших ГАЕС Європи, працює в турбінному та насосному режимах.", ago(21)),
            HydroObject("tashlyk-pspp", "Ташлицька ГАЕС", PSPP, "Південний Буг", "Южноукраїнськ", 47.793, 31.205, 302.0, 0.0, MAINTENANCE,
                "Плановий ремонт гідроагрегату, потужність у резерві.", ago(180)),
            HydroObject("terebla-hpp", "Теребле-Ріцька ГЕС", SMALL_HPP, "Теребля", "Тереблече", 48.538, 23.546, 27.0, 24.0, RUNNING,
                "Дериваційна ГЕС Карпатського регіону.", ago(5))
        )
    }
}

class EmptyHydroObjectSource : HydroObjectSource {
    override suspend fun load(): List<HydroObject> = emptyList()
}
