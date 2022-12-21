package com.sgribkov.socialnetwork

import com.sgribkov.socialnetwork.http.Server
import zio.logging.log
import zio.{ExitCode, URIO}


object Main extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.run
      .tapError(err => log.error(s"Execution failed with: $err"))
      .provideCustomLayer(appLayers)
      .exitCode
}
