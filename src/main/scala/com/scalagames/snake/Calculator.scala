package com.scalagames.snake

import android.graphics.Color
import android.os.{Handler, Looper, Message}
import android.util.Log

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Random

object Calculator {
  sealed trait Event

  sealed trait UserCommand extends Event
  case object MoveLeft extends UserCommand
  case object MoveRight extends UserCommand
  case object MoveDown extends UserCommand
  case object MoveUp extends UserCommand

  case object Pause extends UserCommand
  case object Resume extends UserCommand

  case object Tick extends Event
  case object GameOver extends Event
}

sealed trait Direction
case object Up extends Direction
case object Right extends Direction
case object Down extends Direction
case object Left extends Direction


class Calculator(gameFieldSize: => GameField.GameFieldSize,
                 ui: UI,
                 period: FiniteDuration,
                 initialGameState: GameState) {

  import Calculator._

  var gameState = initialGameState

  private def gameOver(): GameState = {
    gameState = gameState.copy(gameOver = true)
    ui.update(gameState)
    gameState
  }

  private def makeNewSnake: Snake = {
    val p = Position(gameFieldSize.width / 2 - 1, gameFieldSize.height / 2 - 1)

    val directions = List(Up, Down, Left, Right)
    val direction = Random.shuffle(directions).head

    Snake(body = List(Block(position = p, color = Color.RED)), direction = direction)
  }

  private def canPlace(snake: Snake): Boolean = {
    val snakeHead = snake.body.head

    (snakeHead.position.x >= 0) &&
      (snakeHead.position.x <= gameFieldSize.width - 1) &&
      (snakeHead.position.y >= 0) &&
      (snakeHead.position.y <= gameFieldSize.height - 1) &&
      (!snake.body.tail.contains(snakeHead)) &&
      (!gameState.obstacles.contains(snakeHead))
  }

  private def movementAllowed(currentDirection: Direction, newDirection: Direction) = (currentDirection, newDirection) match {
    case (Up, Down) => false
    case (Down, Up) => false
    case (Left, Right) => false
    case (Right, Left) => false
    case _ => true
  }

  private def moveSnake(snake: Snake, direction: Direction): GameState = {
    val movedSnake = snake.move(direction)
    val nextHeadPosition = movedSnake.body.head

    if (gameState.food.contains(nextHeadPosition)) {
      Log.e("calc", s"eating food at ${nextHeadPosition.position}")
      gameState.copy(snake = Some(snake.eat(nextHeadPosition).copy(direction = direction)),
        food = gameState.food.filter(_ != nextHeadPosition),
        score = gameState.score + 1)
    }
    else {
      if (canPlace(movedSnake)) gameState.copy(snake = Some(movedSnake)) else gameOver()
    }
  }

  def handleUserCommand(cmd: UserCommand) {
    gameState = (cmd, gameState.snake) match {
      case (_, None) => gameState.copy(snake = Some(makeNewSnake))
      case (MoveUp, Some(s))    if movementAllowed(s.direction, Up)    => moveSnake(s, Up)
      case (MoveDown, Some(s))  if movementAllowed(s.direction, Down)  => moveSnake(s, Down)
      case (MoveLeft, Some(s))  if movementAllowed(s.direction, Left)  => moveSnake(s, Left)
      case (MoveRight, Some(s)) if movementAllowed(s.direction, Right) => moveSnake(s, Right)
      case (Pause, _)  => gameState.copy(paused = true)
      case (Resume, _) => gameState.copy(paused = false)
      case _ => gameState
    }

    ui.update(gameState)
  }

  @tailrec
  private def createFood(): Block = {
    val foodPosition = Position(x = Random.shuffle(0 to gameFieldSize.width - 1).toList.head,
      y = Random.shuffle(0 to gameFieldSize.height - 1).toList.head)
    val food = Block(color = Color.GREEN, position = foodPosition)

    if (gameState.snake.map(_.body).exists(_.contains(food)) ||
      gameState.food.contains(food))
      createFood()
    else food
  }

  def handleTick(): Unit = {
    gameState = gameState.copy(elapsedTime = gameState.elapsedTime + period)
    handleTickMovement()
    if (gameState.elapsedTime.toSeconds % 10 == 0) {
      gameState = gameState.copy(food = createFood :: gameState.food)
    }
  }

  private def handleTickMovement(): Unit = gameState.snake.map(_.direction) match {
    case Some(Up) => handleUserCommand(MoveUp)
    case Some(Down) => handleUserCommand(MoveDown)
    case Some(Left) => handleUserCommand(MoveLeft)
    case Some(Right) => handleUserCommand(MoveRight)
    case _ => handleUserCommand(MoveDown)
  }
}

class CalculatorThread(gameFieldSize: => GameField.GameFieldSize, ui: UI, period: FiniteDuration, initialGameState: GameState) extends Runnable {

  import Calculator._

  var handler: Option[Handler] = None
  val calculator = new Calculator(gameFieldSize, ui, period, initialGameState)

  private def scheduleTick() {
    val m = new Message
    m.obj = Tick
    handler.foreach(_.sendMessageDelayed(m, period.toMillis))
  }

  override def run() {
    Looper.prepare()
    handler = Some(new Handler {
      override def handleMessage(msg: Message) = msg.obj match {
        case m: UserCommand if !calculator.gameState.gameOver =>
          if (m.isInstanceOf[Resume.type])
            scheduleTick()
          calculator.handleUserCommand(m)
        case Tick           if  calculator.gameState.paused   => /** nothing to do */
        case Tick           if !calculator.gameState.gameOver =>
          calculator.handleTick()
          scheduleTick()
        case _        =>
      }
    })
    scheduleTick()
    Looper.loop()
  }
}
