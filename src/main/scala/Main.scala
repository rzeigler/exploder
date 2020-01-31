import cats._
import cats.effect._
import cats.implicits._
import fs2._
import scala.concurrent.duration._

object Main extends IOApp {
  val mayThrowOOME: IO[Array[Int]] = IO.delay {
    val r = math.random()
    if (r < 0.1d) new Array[Int](Int.MaxValue)
    else new Array[Int]((r * 100).toInt)
  } recoverWith {
    case _: Throwable => IO(println("explode")).as(new Array[Int](0))
  }

  val mayThrowRTE: IO[Array[Int]] = IO.delay {
    val r = math.random()
    if (r < 0.1d) throw new RuntimeException("boom")
    else new Array[Int]((r * 100).toInt)
  } recoverWith {
    case _: NumberFormatException =>
      IO(println("explode")).as(new Array[Int](0))
  }

  val mayThrowRTECatch: IO[Array[Int]] = IO.delay {
    val r = math.random()
    if (r < 0.1d) throw new RuntimeException("boom")
    else new Array[Int]((r * 100).toInt)
  } recoverWith {
    case _: Throwable =>
      IO(println("explode")).as(new Array[Int](0))
  }

  def run(args: List[String]): IO[ExitCode] = {
    val stream =
      if (args.head === "oome")
        Stream.fixedRate(100.millis).evalMap(_ => mayThrowOOME)
      else if (args.head === "rte")
        Stream.fixedRate(100.millis).evalMap(_ => mayThrowRTE)
      else if (args.head === "rte-catch")
        Stream.fixedRate(100.millis).evalMap(_ => mayThrowRTECatch)
      else
        Stream.raiseError[IO](new RuntimeException("bad argument"))
    stream
      .holdResource(new Array[Int](0))
      .use(current =>
        Stream
          .fixedRate(50.millis)
          .evalMap(_ => current.get.flatMap(l => IO(println(l.length))))
          .compile
          .drain
      )
      .as(ExitCode.Success)
  }
}
