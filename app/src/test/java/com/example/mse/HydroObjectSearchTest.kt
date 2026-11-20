package com.example.mse

import com.example.mse.logic.applyQuery
import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectQuery
import com.example.mse.model.ObjectSort
import com.example.mse.model.OperatingStatus
import com.example.mse.ui.screens.task15.HydroObjectSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HydroObjectSearchTest {
    private val items = listOf(
        testObject("kyiv", "Київська ГЕС", HydroObjectType.HPP, 400.0),
        testObject("kaniv", "Канівська ГЕС", HydroObjectType.HPP, 450.0),
        testObject("kyiv-p", "Київська ГАЕС", HydroObjectType.PSPP, 235.0),
        testObject("dnipro", "Дніпровська ГЕС", HydroObjectType.HPP, 1569.0, OperatingStatus.REDUCED),
        testObject("tashlyk", "Ташлицька ГАЕС", HydroObjectType.PSPP, 302.0, OperatingStatus.MAINTENANCE)
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `багаторівнева фільтрація тип + статус + потужність і сортування за потужністю за спаданням`() {
        val query = ObjectQuery(
            types = setOf(HydroObjectType.HPP),
            statuses = setOf(OperatingStatus.RUNNING),
            minCapacityMw = 300.0,
            sort = ObjectSort.POWER,
            descending = true
        )

        assertEquals(listOf("kaniv", "kyiv"), items.applyQuery(query).map { it.id })
        assertEquals("пошук додається четвертим рівнем", listOf("kyiv"), items.applyQuery(query.copy(search = "київ")).map { it.id })
    }

    @Test
    fun `пошук застосовується після debounce, а підсумок вибраного рахує потужність і приховані фільтром`() = runTest {
        val vm = HydroObjectSearchViewModel(FakeObjectSource(items))

        vm.setSearch("київ")
        assertEquals("до закінчення debounce список не змінюється", 5, vm.uiState.value.visible.size)
        advanceTimeBy(300)
        assertEquals(listOf("kyiv", "kyiv-p"), vm.uiState.value.visible.map { it.id }.sorted())

        vm.toggleSelected("kyiv")
        vm.toggleSelected("tashlyk")
        assertEquals(400.0 + 302.0, vm.uiState.value.selectedCapacityMw, 0.001)
        assertEquals("Ташлицька не проходить пошук", 1, vm.uiState.value.hiddenSelected)
    }
}
