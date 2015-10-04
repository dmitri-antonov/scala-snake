package com.scalagames.snake

import android.content.Context
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.{MotionEvent, View, GestureDetector}
import android.view.View.OnTouchListener

class SwipeListener(context: Context) extends OnTouchListener {
  val gestureDetector = new GestureDetector(context, new GestureListener)

  override def onTouch(v: View, e: MotionEvent) = gestureDetector.onTouchEvent(e)

  def onSwipeLeft()  {}
  def onSwipeRight() {}
  def onSwipeUp()    {}
  def onSwipeDown()  {}

  class GestureListener extends SimpleOnGestureListener {
    val SWIPE_DISTANCE_THRESHOLD = 100
    val SWIPE_VELOCITY_THRESHOLD = 100

    override def onDown(e: MotionEvent): Boolean = true

    override def onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = {
      val distanceX = e2.getX - e1.getX
      val distanceY = e2.getY - e1.getY

      if (Math.abs(distanceX) > Math.abs(distanceY)) {
        if (Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD &&
          Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
          if (distanceX > 0) onSwipeRight() else onSwipeLeft()
          true
        }
        else false
      }
      else {
        if (Math.abs(distanceY) > SWIPE_DISTANCE_THRESHOLD &&
          Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
          if (distanceY > 0) onSwipeDown() else onSwipeUp()
          true
        }
        else false
      }
    }
  }
}
