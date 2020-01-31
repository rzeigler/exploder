import cats._
import cats.effect._
import cats.implicits._
import fs2._
import scala.concurrent.duration._

object Main extends IOApp {
  val mayThrow: IO[Array[Int]] = IO.delay {
    val r = math.random()
    if (r < 0.1d) new Array[Int](Int.MaxValue)
    else new Array[Int]((r * 100).toInt)
  }

  val stream = Stream
    .fixedRate(100.millis)
    .evalMap(_ =>
      mayThrow.recoverWith({
        case _: Throwable => IO(println("explode")).as(new Array[Int](0))
      })
    )

  def run(args: List[String]): IO[ExitCode] =
    stream
      .holdResource(new Array[Int](0))
      .use(current =>
        Stream
          .fixedRate(10.millis)
          .evalMap(_ => current.get.flatMap(l => IO(println(l.length))))
          .compile
          .drain
      )
      .as(ExitCode.Success)

}
