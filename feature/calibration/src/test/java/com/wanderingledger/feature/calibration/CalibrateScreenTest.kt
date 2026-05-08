package com.wanderingledger.feature.calibration

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class FakeCalibrationActions : CalibrationActions {
    var startAutoCalled = false
    var stopAutoCalled = false
    var adjustSensitivityCalled = false
    var saveCalibrationCalled = false
    var skipCalibrationCalled = false

    override fun startAuto() { startAutoCalled = true }
    override fun stopAuto() { stopAutoCalled = true }
    override fun adjustSensitivity(value: Float) { adjustSensitivityCalled = true }
    override fun saveCalibration() { saveCalibrationCalled = true }
    override fun skipCalibration() { skipCalibrationCalled = true }
}

@Preview
@Composable
private fun CalibrateScreenPreview() {
    Surface {
        CalibrateScreen(
            state = CalibrationScreenState(
                isRunning = false,
                detectedSteps = 0,
                sensitivity = 1.0f,
                confidence = "High",
                lastCalibratedAt = System.currentTimeMillis() - 3600_000,
                stepCounterAvailable = false,
            ),
            onActions = object : CalibrationActions {
                override fun startAuto() {}
                override fun stopAuto() {}
                override fun adjustSensitivity(value: Float) {}
                override fun saveCalibration() {}
                override fun skipCalibration() {}
            },
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CalibrateScreenRunningPreview() {
    Surface {
        CalibrateScreen(
            state = CalibrationScreenState(
                isRunning = true,
                detectedSteps = 42,
                sensitivity = 1.0f,
                confidence = "Calculating...",
                stepCounterAvailable = false,
            ),
            onActions = object : CalibrationActions {
                override fun startAuto() {}
                override fun stopAuto() {}
                override fun adjustSensitivity(value: Float) {}
                override fun saveCalibration() {}
                override fun skipCalibration() {}
            },
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun CalibrateScreenWithStepCounterPreview() {
    Surface {
        CalibrateScreen(
            state = CalibrationScreenState(
                isRunning = false,
                detectedSteps = 0,
                sensitivity = 0.8f,
                confidence = "Low",
                lastCalibratedAt = System.currentTimeMillis() - 86400_000,
                stepCounterAvailable = true,
            ),
            onActions = object : CalibrationActions {
                override fun startAuto() {}
                override fun stopAuto() {}
                override fun adjustSensitivity(value: Float) {}
                override fun saveCalibration() {}
                override fun skipCalibration() {}
            },
            onBack = {},
        )
    }
}