package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.inference.InferenceConfig
import com.example.inference.OnDeviceInferenceEngine
import com.example.models.ModelManager
import com.example.models.ModelStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Synapse", appName)
    }

    @Test
    fun `test model manager default catalog and active model`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val modelManager = ModelManager(context)
        val activeModel = modelManager.getActiveModelInfo()

        assertNotNull(activeModel)
        assertEquals("nanollm-embedded", activeModel.id)
        assertTrue(activeModel.isEmbedded)
    }

    @Test
    fun `test on device inference streaming`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OnDeviceInferenceEngine(context)
        val modelManager = ModelManager(context)
        val activeModel = modelManager.getActiveModelInfo()

        val loaded = engine.loadModel(activeModel)
        assertTrue(loaded)

        val chunks = engine.generateStream(
            prompt = "Hello",
            config = InferenceConfig(maxTokens = 20)
        ).toList()

        assertTrue(chunks.isNotEmpty())
        val finalChunk = chunks.last()
        assertTrue(finalChunk.isFinished)
        assertTrue(finalChunk.accumulatedText.isNotEmpty())
        assertTrue(finalChunk.telemetry.tokensGenerated > 0)
    }

    @Test
    fun `test weather query detection and location extraction`() {
        val prompt = "hows the weather in haßloch germany"
        assertTrue(com.example.services.WeatherService.isWeatherQuery(prompt))
        val location = com.example.services.WeatherService.extractLocation(prompt)
        assertTrue(location.contains("Haßloch", ignoreCase = true) || location.contains("Hassloch", ignoreCase = true))
    }

    @Test
    fun `test weather offline response formatting`() {
        val text = com.example.services.WeatherService.formatOfflineWeatherKnowledge("Haßloch Germany")
        assertTrue(text.contains("Haßloch") || text.contains("Palatinate"))
        assertTrue(text.contains("°C"))
    }
}
