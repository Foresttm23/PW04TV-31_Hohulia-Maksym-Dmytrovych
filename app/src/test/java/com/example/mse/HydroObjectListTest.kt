package com.example.mse

import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectSort
import com.example.mse.ui.screens.task14.HydroObjectListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HydroObjectListTest {
    private val items = listOf(
        testObject("a", "Альфа ГЕС", HydroObjectType.HPP, 300.0),
        testObject("b", "Бета ГАЕС", HydroObjectType.PSPP, 900.0),
        testObject("c", "Гамма ГЕС", HydroObjectType.HPP, 600.0)
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `клік по маркеру вибирає об'єкт і просить прокрутити список, клік по картці прокрутки не просить`() = runTest {
        val vm = HydroObjectListViewModel(FakeObjectSource(items))

        vm.select("a")
        assertEquals("a", vm.uiState.value.selectedId)
        assertNull(vm.uiState.value.listScrollTarget)

        vm.selectFromMap("c")
        assertEquals("c", vm.uiState.value.selectedId)
        assertEquals("c", vm.uiState.value.listScrollTarget)

        vm.onScrolled()
        assertNull(vm.uiState.value.listScrollTarget)
    }

    @Test
    fun `фільтр за типом і сортування за потужністю, вибір скидається коли об'єкт випав з фільтра`() = runTest {
        val vm = HydroObjectListViewModel(FakeObjectSource(items))
        vm.select("b")

        vm.toggleType(HydroObjectType.HPP)
        vm.setSort(ObjectSort.POWER)
        vm.toggleDirection()

        assertEquals(listOf("c", "a"), vm.uiState.value.visible.map { it.id })
        assertNull("ГАЕС відфільтровано — підсвічування знімається", vm.uiState.value.selectedId)

        vm.toggleType(HydroObjectType.HPP)
        vm.toggleType(HydroObjectType.SMALL_HPP)
        assertTrue(vm.uiState.value.visible.isEmpty())
        assertEquals("порожній результат фільтра ≠ порожнє джерело", 3, vm.uiState.value.items.size)
    }
}
