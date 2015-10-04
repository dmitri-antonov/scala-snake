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
object Up extends Direction
object Right extends Direction
object Down extends Direction
object Left extends Direction

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

case class GameState(snake:       Option[Snake]  = None,
                     food:        List[Block]    = Nil,
                     obstacles:   List[Block]    = Nil,
                     gameOver:    Boolean        = false,
                     paused:      Boolean        = false,
                     elapsedTime: FiniteDuration = 0 seconds,
                     score:       Int            = 0)

class Calculator(gameFieldSize: => GameField.GameFieldSize, uiHandler: Handler, period: FiniteDuration) extends Runnable {

  import Calculator._

  var handler: Option[Handler] = None

  private var gameState = GameState(snake = None)

  private def updateUI(gameState: GameState) {
    val m = new Message
    m.obj = GameField.UiChanged(gameState)
    uiHandler sendMessage m
  }

  private def gameOver() {
    val m = new Message
    m.obj = GameOver
    handler.foreach(_ sendMessage m)
  }

  private def tick() {
    val m = new Message
    m.obj = Tick
    handler.foreach(_.sendMessageDelayed(m, period.toMillis))
  }

  private def makeNewSnake: Snake = {
    val p = Position(gameFieldSize.width / 2 - 1, gameFieldSize.height / 2 - 1)

    val directions = List(Up, Down, Left, Right)
    val direction  = Random.shuffle(directions).head

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
    case (Up, Down)    => false
    case (Down, Up)    => false
    case (Left, Right) => false
    case (Right, Left) => false
    case _             => true
  }

  private def moveSnake(snake: Snake, direction: Direction): GameState = {
    val movedSnake       = snake.move(direction)
    val nextHeadPosition = movedSnake.body.head

    if (gameState.food.contains(nextHeadPosition)) {
      Log.e("calc", s"eating food at ${nextHeadPosition.position}")
      gameState.copy(snake = Some(snake.eat(nextHeadPosition).copy(direction = direction)),
        food   = gameState.food.filter(_ != nextHeadPosition),
        score  = gameState.score + 1)
    }
    else {
      gameState.copy(snake = Some(if (canPlace(movedSnake)) movedSnake else { gameOver(); snake }))
    }
  }

  private def handleUserCommand(cmd: UserCommand) {
    gameState = (cmd, gameState.snake) match {
      case (_, None)            => gameState.copy(snake = Some(makeNewSnake))
      case (MoveUp,    Some(s)) if movementAllowed(s.direction, Up)    => moveSnake(s, Up)
      case (MoveDown,  Some(s)) if movementAllowed(s.direction, Down)  => moveSnake(s, Down)
      case (MoveLeft,  Some(s)) if movementAllowed(s.direction, Left)  => moveSnake(s, Left)
      case (MoveRight, Some(s)) if movementAllowed(s.direction, Right) => moveSnake(s, Right)
      case (Pause, _)  => gameState.copy(paused = true)
      case (Resume, _) => tick(); gameState.copy(paused = false)
      case _ => gameState
    }

    updateUI(gameState)
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

  private def handleTick(): Unit = {
    handleTickMovement()
    if (gameState.elapsedTime.toSeconds % 10 == 0) {
      gameState = gameState.copy(food = createFood :: gameState.food)
    }
  }

  private def handleTickMovement(): Unit = gameState.snake.map(_.direction) match {
    case Some(Up)    => handleUserCommand(MoveUp)
    case Some(Down)  => handleUserCommand(MoveDown)
    case Some(Left)  => handleUserCommand(MoveLeft)
    case Some(Right) => handleUserCommand(MoveRight)
    case _           => handleUserCommand(MoveDown)
  }

  override def run() {
    Looper.prepare()
    handler = Some(new Handler {
      override def handleMessage(msg: Message) = msg.obj match {
        case m: UserCommand if !gameState.gameOver => handleUserCommand(m)
        case Tick           if  gameState.paused   => /** nothing to do */
        case Tick           if !gameState.gameOver =>
          gameState = gameState.copy(elapsedTime = gameState.elapsedTime + period)
          handleTick()
          tick()
        case GameOver => gameState = gameState.copy(gameOver = true); updateUI(gameState)
        case _        =>
      }
    })
    tick()
    Looper.loop()
  }
}
