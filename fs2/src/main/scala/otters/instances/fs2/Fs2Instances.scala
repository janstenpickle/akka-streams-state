package otters.instances.fs2

import _root_.fs2.{Stream => Fs2Stream}
import cats.effect.Effect
import otters.{EitherStreamFunctionPipeSink, FunctionPipe, FunctionSink}

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

trait Fs2Instances {
  implicit def fs2instances[F[_]](implicit F: Effect[F]): EitherStreamFunctionPipeSink[Fs2Stream[F, ?], F, F] =
    new EitherStreamFunctionPipeSink[Fs2Stream[F, ?], F, F] {
      override def map[A, B](fa: Fs2Stream[F, A])(f: A => B): Fs2Stream[F, B] = fa.map(f)

      override def mapAsync[A, B](fa: Fs2Stream[F, A])(f: A => F[B]): Fs2Stream[F, B] =
        fa.evalMap(f)

      override def mapAsyncN[A, B](fa: Fs2Stream[F, A])(parallelism: Int)(f: A => F[B]): Fs2Stream[F, B] =
        mapAsync(fa)(f)

      override def grouped[A](fa: Fs2Stream[F, A])(count: Int): Fs2Stream[F, Seq[A]] =
        fa.chunkLimit(count).map(_.toVector).filter(_.nonEmpty)

      override def groupedWithin[A](fa: Fs2Stream[F, A])(count: Int, timespan: FiniteDuration): Fs2Stream[F, Seq[A]] =
        grouped(fa)(count)

      override def flatMap[A, B](fa: Fs2Stream[F, A])(f: A => Fs2Stream[F, B]): Fs2Stream[F, B] = fa.flatMap(f)

      override def mapConcat[A, B](fa: Fs2Stream[F, A])(f: A => immutable.Iterable[B]): Fs2Stream[F, B] =
        fa.flatMap(a => Fs2Stream.apply(f(a).toVector: _*))

      override def fromIterator[A](iter: => Iterator[A]): Fs2Stream[F, A] = Fs2Stream.emits(iter.toSeq)

      override def pure[A](x: A): Fs2Stream[F, A] = Fs2Stream(x)

      override def ap[A, B](ff: Fs2Stream[F, A => B])(fa: Fs2Stream[F, A]): Fs2Stream[F, B] = ff.flatMap(fa.map)

      override def fromSeq[A](seq: Seq[A]): Fs2Stream[F, A] = Fs2Stream.emits(seq)

      override def zip[A, B](fa: Fs2Stream[F, A])(fb: Fs2Stream[F, B]): Fs2Stream[F, (A, B)] = fa.zip(fb)

      override def collect[A, B](fa: Fs2Stream[F, A])(pf: PartialFunction[A, B]): Fs2Stream[F, B] = fa.collect(pf)

      override def tailRecM[A, B](a: A)(f: A => Fs2Stream[F, Either[A, B]]): Fs2Stream[F, B] =
        Fs2Stream.syncInstance[F].tailRecM(a)(f)

      override def toEitherSinks[A, B, C, D, E](fab: Fs2Stream[F, Either[A, B]])(
        lSink: FunctionSink[fs2.Stream[F, ?], F, A, C],
        rSink: FunctionSink[fs2.Stream[F, ?], F, B, D]
      )(combine: (C, D) => E): F[E] = {
        val l = lSink(fab.collect { case Left(a) => a })
        val r = rSink(fab.collect { case Right(b) => b })

        F.flatMap(l)(ll => F.map(r)(combine(ll, _)))
      }

      override def toSinks[A, B, C, D, E](fab: Fs2Stream[F, (A, B)])(
        lSink: FunctionSink[fs2.Stream[F, ?], F, A, C],
        rSink: FunctionSink[fs2.Stream[F, ?], F, B, D]
      )(combine: (C, D) => E): F[E] = {
        val l = lSink(fab.map(_._1))
        val r = rSink(fab.map(_._2))

        F.flatMap(l)(ll => F.map(r)(combine(ll, _)))
      }

      override def fanOutFanIn[A, B, C, D](fab: Fs2Stream[F, (A, B)])(
        lPipe: FunctionPipe[fs2.Stream[F, ?], A, C],
        rPipe: FunctionPipe[fs2.Stream[F, ?], B, D]
      ): Fs2Stream[F, (C, D)] = {
        val l = lPipe(fab.map(_._1))
        val r = rPipe(fab.map(_._2))

        zip(l)(r)
      }
    }
}
