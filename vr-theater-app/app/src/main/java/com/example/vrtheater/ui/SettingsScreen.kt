package com.example.vrtheater.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vrtheater.settings.SettingsRepository
import com.example.vrtheater.settings.VRSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repo: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsState(initial = VRSettings())

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Calibration", style = MaterialTheme.typography.headlineSmall)

        LabeledSlider(
            label = "Eye Separation",
            value = settings.eyeSeparation,
            valueRange = 0.0f..0.08f,
            onChange = { v -> scope.launch { repo.update { it.copy(eyeSeparation = v) } } }
        )
        LabeledSlider(
            label = "Barrel k1",
            value = settings.k1,
            valueRange = 0.0f..0.5f,
            onChange = { v -> scope.launch { repo.update { it.copy(k1 = v) } } }
        )
        LabeledSlider(
            label = "Barrel k2",
            value = settings.k2,
            valueRange = 0.0f..0.5f,
            onChange = { v -> scope.launch { repo.update { it.copy(k2 = v) } } }
        )
        LabeledSlider(
            label = "Screen Scale",
            value = settings.screenScale,
            valueRange = 0.5f..1.5f,
            onChange = { v -> scope.launch { repo.update { it.copy(screenScale = v) } } }
        )
        LabeledSlider(
            label = "Screen Tilt",
            value = settings.screenTilt,
            valueRange = -0.3f..0.3f,
            onChange = { v -> scope.launch { repo.update { it.copy(screenTilt = v) } } }
        )
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ${String.format("%.3f", value)}")
        Slider(value = value, onValueChange = onChange, valueRange = valueRange)
    }
}