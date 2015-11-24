package com.scalagames.snake

import com.scalagames.snake.GameField.GameFieldSize
import org.scalatest.{FlatSpec, Matchers, RobolectricSuite}
import org.scalamock.scalatest.MockFactory
import scala.concurrent.duration._

class SnakeSpec extends FlatSpec with Matchers with MockFactory with RobolectricSuite {

  /**
   *
   * o - empty
   * x - snake body
   * > - snake head (looks rightward)
   *
   * o x o o o
   * o x o o o
   * o x > o o
   * o o o o o
   * o o o o o
   *
   */

  import Calculator._

  val uiMock = mock[UI]
  val gameFieldSize = GameFieldSize(width = 5, height = 5)
  val snake = Snake(body = List(Block(Position(2, 2)), Block(Position(1, 2)), Block(Position(1, 1)), Block(Position(1, 0))),
                    direction = Right)
  val initialGameState = GameState(snake = Some(snake))

  "Snake" should "move properly" in {
    val calculator = new Calculator(gameFieldSize, uiMock, period = 1 second, initialGameState)

    val movedSnake = Snake(body = List(Block(Position(4, 4)), Block(Position(4, 3)), Block(Position(4, 2)), Block(Position(3, 2))),
                           direction = Down)

    (uiMock.update _).expects(*).repeated(4).times

    calculator.handleTick()
    calculator.handleTick()
    calculator.handleUserCommand(MoveDown)
    calculator.handleTick()

    calculator.gameState.snake shouldBe Some(movedSnake)
    calculator.gameState.gameOver shouldEqual false
  }

  "Snake" should "grow when it eats food" in {

    /**
     *
     * o - empty
     * x - snake body
     * > - snake head (looks rightward)
     * F - food
     *
     * o x o o o
     * o x o o o
     * o x > o F
     * o o o o o
     * o o o o o
     *
     */

    val calculator = new Calculator(gameFieldSize, uiMock, period = 1 second, initialGameState.copy(food = List(Block(Position(4, 2)))))

    val movedSnake = Snake(body = List(Block(Position(4, 2)), Block(Position(3, 2)), Block(Position(2, 2)), Block(Position(1, 2)), Block(Position(1, 1))),
                           direction = Right)

    (uiMock.update _).expects(*).repeated(2).times

    calculator.handleTick()
    calculator.handleTick()

    calculator.gameState.snake shouldBe Some(movedSnake)
    calculator.gameState.gameOver shouldEqual false
    calculator.gameState.score shouldEqual 1
  }

  "Game" should "be over when the snake hits a game field boundary" in {
    val calculator = new Calculator(gameFieldSize, uiMock, period = 1 second, initialGameState)

    val movedSnake = Snake(body = List(Block(Position(4, 2)), Block(Position(3, 2)), Block(Position(2, 2)), Block(Position(1, 2))),
      direction = Right)

    (uiMock.update _).expects(*).repeated(4).times

    calculator.handleTick()
    calculator.handleTick()
    calculator.handleTick()

    calculator.gameState.snake shouldBe Some(movedSnake)
    calculator.gameState.gameOver shouldEqual true
  }

  "Game" should "be over when the snake hits it's own tail" in {
    /**
     *
     * o - empty
     * x - snake body
     * < - snake head (looks leftward)
     *
     * o x o o o
     * o x < o o
     * o x x o o
     * o o o o o
     * o o o o o
     *
     */

    val snake = Snake(body = List(Block(Position(2, 1)), Block(Position(2, 2)), Block(Position(1, 2)), Block(Position(1, 1)), Block(Position(1, 0))),
                      direction = Left)

    val calculator = new Calculator(gameFieldSize, uiMock, period = 1 second, GameState(snake = Some(snake)))

    val movedSnake = Snake(body = List(Block(Position(4, 2)), Block(Position(3, 2)), Block(Position(2, 2)), Block(Position(1, 2)), Block(Position(1, 1))),
      direction = Right)

    (uiMock.update _).expects(*).repeated(2).times

    calculator.handleTick()

    calculator.gameState.snake shouldBe Some(snake)
    calculator.gameState.gameOver shouldEqual true
  }
}
