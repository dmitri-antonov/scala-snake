package com.scalagames.snake

import scala.concurrent.duration._

case class GameState(snake:       Option[Snake]  = None,
                     food:        List[Block]    = Nil,
                     obstacles:   List[Block]    = Nil,
                     gameOver:    Boolean        = false,
                     paused:      Boolean        = false,
                     elapsedTime: FiniteDuration = 0 seconds,
                     score:       Int            = 0)
