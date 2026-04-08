package com.example.drivealert.sensores

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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
            SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private var lastAcceleration = 0f
    private var filteredAcceleration = 0f

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val currentAcceleration = sqrt(x * x + y * y + z * z)

        val deltaFromGravity = kotlin.math.abs(currentAcceleration - 9.81f)

        filteredAcceleration = filteredAcceleration * 0.95f + deltaFromGravity

        isMoving = filteredAcceleration > 0.3f
        Log.d("SENSOR", "isMoving: $isMoving filteredAcceleration: $filteredAcceleration")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}