package com.scalagames.snake

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.scalagames.snake.Calculator.{Resume, Pause}

//import android.widget.Toast

class SnakeActivity extends Activity {

  def gameField = findViewById(R.id.gameField).asInstanceOf[GameField]

  private var once = false

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)
    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    setContentView(R.layout.main)
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    //Toast.makeText(getBaseContext, "onDestroy", Toast.LENGTH_LONG).show()
  }

  override def onStart(): Unit = {
    super.onStart()
    //Toast.makeText(getBaseContext, "onStart", Toast.LENGTH_LONG).show()
  }

  override def onStop(): Unit = {
    super.onStop()
    //Toast.makeText(getBaseContext, "onStop", Toast.LENGTH_LONG).show()
  }

  override def onRestart(): Unit = {
    super.onRestart()
    //Toast.makeText(getBaseContext, "onRestart", Toast.LENGTH_LONG).show()
  }

  override def onPause() = {
    super.onPause()
    once = true
    gameField.notifyCalculator(Pause)
    //Toast.makeText(getBaseContext, "onPause", Toast.LENGTH_LONG).show()
  }

  override def onResume(): Unit = {
    super.onResume()
    if (once)
      gameField.notifyCalculator(Resume)
    //Toast.makeText(getBaseContext, "onResume", Toast.LENGTH_LONG).show()
  }
}
