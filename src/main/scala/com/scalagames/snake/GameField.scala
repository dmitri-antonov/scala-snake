package com.scalagames.snake

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import android.content.Context
import android.graphics.Paint.Align
import android.graphics.{Color, Paint, Canvas}
import android.os.{Message, Looper, Handler}
import android.util.{AttributeSet, Log}
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.{GestureDetector, MotionEvent, View}
import android.view.View.OnTouchListener
import android.widget.{LinearLayout, TextView}

import scala.concurrent.duration._

object GameField {
  case class UiChanged(newGameState: GameState)
  case object Restart

  case class GameFieldSize(width: Int, height: Int)
  case class CalculatorState(calculator: Calculator, thread: Thread)
}

class GameField(context: Context, attrs: AttributeSet) extends View(context, attrs) {

  val TAG = "snake"

  import GameField._

  val margin         = 1
  val widthInBlocks  = 20
  val blockMargin    = 1

  lazy val heightInPixels = getHeight
  lazy val widthInPixels  = getWidth

  lazy val blockSize      = (widthInPixels - 2 * margin) / widthInBlocks
  lazy val heightInBlocks = (heightInPixels - 2 * margin) / blockSize

  lazy val horizontalPadding = (widthInPixels - 2 * margin) % blockSize
  lazy val verticalPadding   = (heightInPixels - 2 * margin) % blockSize

  lazy val leftPadding   = horizontalPadding / 2
  lazy val rightPadding  = horizontalPadding - leftPadding
  lazy val topPadding    = verticalPadding / 2
  lazy val bottomPadding = verticalPadding - topPadding

  val boundaryColor    = Color.RED
  val fieldColor       = Color.BLACK
  val blockMarginColor = Color.WHITE

  private var gameState = GameState()

  val uiHandler = new Handler(Looper.getMainLooper) {
    override def handleMessage(msg: Message) = msg.obj match {
      case UiChanged(newGameState) => updateUI(Some(newGameState))
      case Restart                 =>
        calculatorState.calculator.handler.flatMap(h => Option(h.getLooper)).foreach(_.quit())
        calculatorState = restartGame()
      case _                       => updateUI(None)
    }
  }

  var calculatorState = restartGame()

  private def restartGame(): CalculatorState = {
    Log.e(TAG, "restarting game")

    val calculator = new Calculator(GameFieldSize(widthInBlocks, heightInBlocks), uiHandler, period = 0.5 second)
    val calcThread = new Thread(calculator)
    calcThread.start()

    setOnTouchListener(new ShapeSwipeListener)
    CalculatorState(calculator, calcThread)
  }

  class ShapeSwipeListener extends SwipeListener(context) {
    override def onSwipeLeft()  { notifyCalculator(Calculator.MoveLeft) }
    override def onSwipeRight() { notifyCalculator(Calculator.MoveRight) }
    override def onSwipeUp()    { notifyCalculator(Calculator.MoveUp) }
    override def onSwipeDown()  { notifyCalculator(Calculator.MoveDown) }
  }

  def notifyCalculator(cmd: Calculator.UserCommand) {
    val m = new Message
    m.obj = cmd
    calculatorState.calculator.handler.foreach(_ sendMessage m)
  }

  private def requestRestartGame() {
    val m = new Message
    m.obj = Restart
    uiHandler sendMessage m
  }

  private def drawBoundary(canvas: Canvas) {
    val paint = new Paint

    paint.setColor(Color.BLACK)
    canvas.drawRect(0, 0, widthInPixels, heightInPixels, paint)

    paint.setColor(boundaryColor)
    canvas.drawRect(leftPadding, topPadding, widthInPixels - rightPadding, heightInPixels - bottomPadding, paint)

    paint.setColor(fieldColor)
    canvas.drawRect(leftPadding + margin, topPadding + margin, widthInPixels - rightPadding - margin, heightInPixels - bottomPadding - margin, paint)
  }

  private def drawBlock(canvas: Canvas, b: Block) {
      val paint = new Paint
      paint.setAntiAlias(true)
      paint.setColor(blockMarginColor)
      canvas.drawRect(b.position.x * blockSize + margin + leftPadding,
                      b.position.y * blockSize + margin + topPadding,
                      b.position.x * blockSize + margin + leftPadding + blockSize,
                      b.position.y * blockSize + margin + topPadding + blockSize, paint)
      paint.setColor(b.color)
      canvas.drawRect(b.position.x * blockSize + margin + leftPadding + blockMargin,
                      b.position.y * blockSize + margin + topPadding + blockMargin,
                      b.position.x * blockSize + margin + leftPadding + blockSize - blockMargin,
                      b.position.y * blockSize + margin + topPadding + blockSize - blockMargin, paint)
  }

  private def drawBlocks(canvas: Canvas, blocks: List[Block]) = blocks foreach (drawBlock(canvas, _))

  private def drawGameOver(canvas: Canvas) {
    val paint = new Paint
    val text  = "GAME OVER"
    val x  = canvas.getWidth / 2
    val y  = (canvas.getHeight / 2) - ((paint.descent + paint.ascent) / 2)

    paint.setColor(Color.RED)
    paint.setTextAlign(Align.CENTER)
    paint.setAntiAlias(true)
    paint.setTextSize(50)
    canvas.drawText(text, x, y, paint)
  }

  override def onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    drawBoundary(canvas)
    gameState.snake.foreach(s => drawBlocks(canvas, s.body))
    drawBlocks(canvas, gameState.food)
    drawBlocks(canvas, gameState.obstacles)

    val elapsedTime = new SimpleDateFormat("mm:ss", Locale.getDefault).format(new Date(gameState.elapsedTime.toMillis))
    val parent      = getParent.asInstanceOf[LinearLayout]

    parent.findViewById(R.id.time).asInstanceOf[TextView].setText(elapsedTime)
    parent.findViewById(R.id.score).asInstanceOf[TextView].setText(gameState.score.toString)

    if (gameState.gameOver) {
      Log.e(TAG, "the game is over")
      drawGameOver(canvas)
      onTouch(requestRestartGame)
    }
  }

  private def onTouch(block: => Unit) {
    setOnTouchListener(new OnTouchListener {
      val gestureDetector = new GestureDetector(context, new SimpleOnGestureListener {
        override def onLongPress(e: MotionEvent) {
          Log.e(TAG, "tap")
          block
        }
      })
      override def onTouch(v: View, e: MotionEvent) = gestureDetector.onTouchEvent(e)
    })
  }

  def updateUI(newGameState: Option[GameState] = None) {
    if (newGameState.isDefined)
      gameState = newGameState.get
    invalidate()
  }
}
