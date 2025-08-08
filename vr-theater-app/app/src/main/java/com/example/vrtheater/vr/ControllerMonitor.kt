package com.example.vrtheater.vr

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice

class ControllerMonitor(context: Context) : InputManager.InputDeviceListener {
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

    @Volatile var connectedControllers: List<Int> = emptyList()
        private set

    fun start() {
        inputManager.registerInputDeviceListener(this, null)
        refresh()
    }

    fun stop() {
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) { refresh() }
    override fun onInputDeviceRemoved(deviceId: Int) { refresh() }
    override fun onInputDeviceChanged(deviceId: Int) { refresh() }

    private fun refresh() {
        val ids = inputManager.inputDeviceIds
        connectedControllers = ids.toList().mapNotNull { id ->
            val dev = inputManager.getInputDevice(id) ?: return@mapNotNull null
            if (isGameController(dev)) id else null
        }
    }

    private fun isGameController(device: InputDevice): Boolean {
        val sources = device.sources
        val hasGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val hasJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        return hasGamepad || hasJoystick
    }
}