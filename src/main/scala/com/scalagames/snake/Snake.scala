package com.scalagames.snake

case class Snake(body:      List[Block],
                 direction: Direction) {

  def move(direction: Direction): Snake = {
    val newHeadPosition = direction match {
      case Up    => body.head.position.up
      case Down  => body.head.position.down
      case Left  => body.head.position.left
      case Right => body.head.position.right
    }
    this.copy(body = body.head.copy(position = newHeadPosition) :: body.init, direction = direction)
  }

  def eat(food: Block): Snake = this.copy(body = Block(position = food.position, color = body.head.color) :: body)
}
