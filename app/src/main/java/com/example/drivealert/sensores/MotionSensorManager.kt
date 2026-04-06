package com.example.drivealert.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class MotionSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var isMoving = false
        private set

    fun start() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private var lastAcceleration = 0f

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val currentAcceleration = sqrt(x * x + y * y + z * z)

        val delta = kotlin.math.abs(currentAcceleration - lastAcceleration)

        lastAcceleration = currentAcceleration

        isMoving = delta > 0.5f || currentAcceleration > 3
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}