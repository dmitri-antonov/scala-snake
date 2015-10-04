package com.scalagames.snake

case class Position(x: Int, y: Int) {
  def up    = Position(x, y - 1)
  def down  = Position(x, y + 1)
  def left  = Position(x - 1, y)
  def right = Position(x + 1, y)
}
