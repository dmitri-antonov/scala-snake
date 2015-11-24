package com.scalagames.snake

import android.hardware.{Sensor, SensorEvent, SensorEventListener}
import android.util.Log

import scala.concurrent.duration._

case class TiltListener(notifyCalculator: Calculator.UserCommand => Unit) extends SensorEventListener {
  var lastTime = System.currentTimeMillis()
  val period   = 300 millis
  override def onSensorChanged(event: SensorEvent) {
    //      val sensor = event.sensor.toString
    //      val acc    = event.accuracy
    //      val time   = event.timestamp

    //      if (gameState.gameOver) {
    //        sensorManager.unregisterListener(this, sensor)
    //        Log.e(TAG, "stopped sensor")
    //      }

    if ((System.currentTimeMillis() - lastTime) > period.toMillis) {

      val x = event.values(0)
      val y = event.values(1)
      val z = event.values(2)

      val diff = 1.5

      Log.e("ACC", s"SENSOR CHANGED: x = $x, y = $y, z = $z")

      if ((Math.abs(x) - Math.abs(y)) > diff) {
        if (x < 0) {
          Log.e("ACC", "TILTED RIGHT")
          //notifyCalculator(Calculator.MoveRight)
          notifyCalculator(Calculator.MoveUp)
        }
        else {
          Log.e("ACC", "TILTED LEFT")
          //notifyCalculator(Calculator.MoveLeft)
          notifyCalculator(Calculator.MoveDown)
        }
      }
      else if ((Math.abs(y) - Math.abs(x)) > diff) {
        if (y < 0) {
          Log.e("ACC", "TILTED UP")
          //notifyCalculator(Calculator.MoveUp)
          notifyCalculator(Calculator.MoveLeft)
        }
        else {
          Log.e("ACC", "TILTED DOWN")
          //notifyCalculator(Calculator.MoveDown)
          notifyCalculator(Calculator.MoveRight)
        }
      }
      else {
        Log.e("ACC", "NO TILT")
      }
    }
  }

  override def onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    Log.e("ACC", s"ACCURACY CHANGED: sensor ${sensor}, accuracy ${accuracy}")
  }
}
