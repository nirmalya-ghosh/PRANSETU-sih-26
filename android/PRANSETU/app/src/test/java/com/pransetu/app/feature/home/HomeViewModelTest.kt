package com.pransetu.app.feature.home

import com.pransetu.app.core.data.repository.SosCanonicalModel
import com.pransetu.app.core.data.repository.toEntity
import com.pransetu.app.core.data.repository.toCanonicalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HomeViewModelTest {

    @Test
    fun canonicalSosModel_jsonSerializationAndDeserialization_preservesData() {
        val sos = SosCanonicalModel(
            sosId = "sos-test-uuid-1234",
            protocolVersion = "1.0",
            createdAt = 1700000000000L,
            source = "android_app",
            deviceIdentifier = "Samsung Galaxy S24 FE",
            latitude = 20.4625,
            longitude = 85.8830,
            locationTimestamp = 1700000000000L,
            locationAccuracy = 4.5f,
            severityCode = 3,
            peopleCount = 4,
            medicalRequired = true,
            hopCount = 2,
            ttl = 62,
            deliveryState = "SERVER_RECEIVED",
            message = "Trapped on rooftop due to flash flood"
        )

        val jsonString = sos.toJson()
        val restored = SosCanonicalModel.fromJson(jsonString)

        assertNotNull(restored)
        assertEquals(sos.sosId, restored?.sosId)
        assertEquals(sos.deviceIdentifier, restored?.deviceIdentifier)
        assertEquals(sos.latitude, restored?.latitude)
        assertEquals(sos.longitude, restored?.longitude)
        assertEquals(sos.severityCode, restored?.severityCode)
        assertEquals(sos.peopleCount, restored?.peopleCount)
        assertEquals(sos.medicalRequired, restored?.medicalRequired)
        assertEquals(sos.hopCount, restored?.hopCount)
        assertEquals(sos.message, restored?.message)
    }

    @Test
    fun sosEntity_conversionToAndFromCanonicalModel_isLossless() {
        val sos = SosCanonicalModel(
            sosId = "sos-conversion-5678",
            latitude = 19.8135,
            longitude = 85.8312,
            severityCode = 2,
            peopleCount = 2,
            medicalRequired = false,
            message = "Bridge collapsed"
        )

        val entity = sos.toEntity()
        val restored = entity.toCanonicalModel()

        assertEquals(sos.sosId, restored.sosId)
        assertEquals(sos.latitude, restored.latitude)
        assertEquals(sos.longitude, restored.longitude)
        assertEquals(sos.peopleCount, restored.peopleCount)
        assertEquals(sos.medicalRequired, restored.medicalRequired)
        assertEquals(sos.message, restored.message)
    }
}
