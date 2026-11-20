package com.example.mse

import com.example.mse.data.source.LocationSource
import com.example.mse.model.GeoPoint
import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectSort
import com.example.mse.ui.screens.task16.HydroMapViewModel
import com.example.mse.ui.screens.task16.LocationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeLocationSource : LocationSource {
    val flow = MutableSharedFlow<GeoPoint>()
    var subscriptions = 0
    override fun updates(): Flow<GeoPoint> {
        subscriptions++
        return flow
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HydroMapViewModelTest {
    private val items = listOf(
        testObject("kyiv", "Київська ГЕС", HydroObjectType.HPP, lat = 50.585, lon = 30.483),
        testObject("kaniv", "Канівська ГЕС", HydroObjectType.HPP, lat = 49.762, lon = 31.495),
        testObject("dnipro", "Дніпровська ГЕС", HydroObjectType.HPP, lat = 47.870, lon = 35.087)
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `рух користувача реактивно перераховує відстані, найближчий об'єкт і сортування за відстанню`() = runTest {
        val location = FakeLocationSource()
        val vm = HydroMapViewModel(FakeObjectSource(items), location)
        vm.setSort(ObjectSort.DISTANCE)

        vm.onPermissionResult(true)
        location.flow.emit(GeoPoint(50.45, 30.52))
        assertEquals("kyiv", vm.uiState.value.nearest?.id)
        assertEquals(listOf("kyiv", "kaniv", "dnipro"), vm.uiState.value.visible.map { it.id })

        location.flow.emit(GeoPoint(47.84, 35.14))
        assertEquals("dnipro", vm.uiState.value.nearest?.id)
        assertEquals(listOf("dnipro", "kaniv", "kyiv"), vm.uiState.value.visible.map { it.id })
        assertNotNull(vm.uiState.value.distanceKm(items[0]))
    }

    @Test
    fun `без дозволу геолокація не запитується, список лишається, а сортування за відстанню деградує до назви`() = runTest {
        val location = FakeLocationSource()
        val vm = HydroMapViewModel(FakeObjectSource(items), location)

        vm.onPermissionResult(false)
        vm.setSort(ObjectSort.DISTANCE)

        assertEquals(LocationPermission.DENIED, vm.uiState.value.permission)
        assertEquals(0, location.subscriptions)
        assertNull(vm.uiState.value.userPosition)
        assertNull(vm.uiState.value.nearest)
        assertEquals(listOf("Дніпровська ГЕС", "Канівська ГЕС", "Київська ГЕС"), vm.uiState.value.visible.map { it.name })
    }
}
