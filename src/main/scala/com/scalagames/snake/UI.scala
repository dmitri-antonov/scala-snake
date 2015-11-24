package com.scalagames.snake

import android.os.{Message, Handler}

trait UI {
  def update(gameState: GameState): Unit
}

case class RealUI(uiHandler: Handler) extends UI {
  def update(gameState: GameState): Unit = {
    val m = new Message
    m.obj = GameField.UiChanged(gameState)
    uiHandler sendMessage m
  }
}

