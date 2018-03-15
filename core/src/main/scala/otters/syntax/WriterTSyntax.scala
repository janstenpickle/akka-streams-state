package otters.syntax

import cats.data.{Writer, WriterT}
import cats.kernel.Semigroup
import cats.syntax.functor._
import cats.{Applicative, Functor, Id, Monoid, Semigroupal}
import otters._
import otters.syntax.stream._

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

trait WriterTSyntax {

  implicit class WriterTStreamOps[F[_], G[_], H[_], L, A](override val stream: WriterT[F, L, A])
      extends WriterTStreamGrouped[F, L, A]
      with WriterTStreamPipeSink[F, G, H, L, A]
      with WriterTStreamAsync[F, G, L, A]
      with WriterTStreamConcatOps[F, L, A]

  implicit class WriterTStreamApply[F[_], A](stream: F[A]) {
    def toWriter[L: Monoid](implicit F: Applicative[F]): WriterT[F, L, A] = WriterT.liftF(stream)
    def toWriter[L](initial: L)(implicit F: Functor[F]): WriterT[F, L, A] = WriterT(stream.map(initial -> _))
  }

  implicit class WriterTStreamApplyTuple[F[_], L, A](stream: F[(L, A)]) {
    def toWriter: WriterT[F, L, A] = WriterT(stream)
  }
}

trait WriterTStreamGrouped[F[_], L, A] {
  import SeqInstances._

  def stream: WriterT[F, L, A]

  def groupedWithin(
    n: Int,
    d: FiniteDuration
  )(implicit F: Stream[F], ev: Applicative[Writer[L, ?]]): WriterT[F, L, Seq[A]] =
    WriterT(stream.run.groupedWithin(n, d).map(las => seqTraverse.sequence(las.map(WriterT[Id, L, A])).run))

  def grouped(n: Int)(implicit F: Stream[F], ev: Applicative[Writer[L, ?]]): WriterT[F, L, Seq[A]] =
    WriterT(stream.run.grouped(n).map(las => seqTraverse.sequence(las.map(WriterT[Id, L, A])).run))
}

trait WriterTStreamPipeSink[F[_], G[_], H[_], L, A] {
  def stream: WriterT[F, L, A]

  def toSinks[B, C, D](lSink: Sink[F, H, L, B], rSink: Sink[F, H, A, C])(
    combine: (B, C) => D
  )(implicit F: TupleStream[F, G, H]): H[D] =
    F.toSinks[L, A, B, C, D](stream.run)(lSink, rSink)(combine)

  def toSinksTupled[B, C](lSink: Sink[F, H, L, B], rSink: Sink[F, H, A, C])(
    implicit F: TupleStream[F, G, H]
  ): H[(B, C)] =
    F.toSinks[L, A, B, C](stream.run)(lSink, rSink)

  def via[B, C](lPipe: Pipe[F, L, B], rPipe: Pipe[F, A, C])(implicit F: TupleStream[F, G, H]): WriterT[F, B, C] =
    WriterT(F.fanOutFanIn(stream.run)(lPipe, rPipe))

  def stateVia[B](lPipe: Pipe[F, L, B])(implicit F: TupleStream[F, G, H]): WriterT[F, B, A] = via(lPipe, identity)
  def dataVia[B](rPipe: Pipe[F, A, B])(implicit F: TupleStream[F, G, H]): WriterT[F, L, B] = via(identity, rPipe)
}

trait WriterTStreamAsync[F[_], G[_], L, A] {
  def stream: WriterT[F, L, A]

  def mapAsync[B](f: A => G[B])(implicit F: AsyncStream[F, G], G: Functor[G]): WriterT[F, L, B] =
    WriterT(F.mapAsync(stream.run)(doMapAsync(f).tupled))

  def mapAsync[B](parallelism: Int)(f: A => G[B])(implicit F: AsyncStream[F, G], G: Functor[G]): WriterT[F, L, B] =
    WriterT(F.mapAsyncN(stream.run)(parallelism: Int)(doMapAsync(f).tupled))

  private def doMapAsync[B](f: A => G[B])(implicit G: Functor[G]): (L, A) => G[(L, B)] =
    (l, a) => f(a).map(l -> _)

  def flatMapAsync[B](
    f: A => WriterT[G, L, B]
  )(implicit F: AsyncStream[F, G], G: Functor[G], L: Semigroup[L]): WriterT[F, L, B] =
    WriterT(F.mapAsync(stream.run)(doFlatMapAsync(f).tupled))

  def flatMapAsync[B](
    parallelism: Int
  )(f: A => WriterT[G, L, B])(implicit F: AsyncStream[F, G], G: Functor[G], L: Semigroup[L]): WriterT[F, L, B] =
    WriterT(F.mapAsyncN(stream.run)(parallelism)(doFlatMapAsync(f).tupled))

  private def doFlatMapAsync[B](
    f: A => WriterT[G, L, B]
  )(implicit G: Functor[G], L: Semigroup[L]): (L, A) => G[(L, B)] =
    (l, a) => f(a).mapWritten(L.combine(_, l)).run

  def mapBothAsync[M, B](f: (L, A) => G[(M, B)])(implicit F: AsyncStream[F, G], G: Functor[G]): WriterT[F, M, B] =
    WriterT(F.mapAsync(stream.run)(f.tupled))

  def mapBothAsync[M, B](
    parallelism: Int
  )(f: (L, A) => G[(M, B)])(implicit F: AsyncStream[F, G], G: Functor[G]): WriterT[F, M, B] =
    WriterT(F.mapAsyncN(stream.run)(parallelism)(f.tupled))

  def bimapAsync[M, B](f: L => G[M], g: A => G[B])(implicit F: AsyncStream[F, G], G: Semigroupal[G]): WriterT[F, M, B] =
    WriterT(F.mapAsync(stream.run)(doBimapAsync(f, g).tupled))

  def bimapAsync[M, B](
    parallelism: Int
  )(f: L => G[M], g: A => G[B])(implicit F: AsyncStream[F, G], G: Semigroupal[G]): WriterT[F, M, B] =
    WriterT(F.mapAsyncN(stream.run)(parallelism)(doBimapAsync(f, g).tupled))

  def doBimapAsync[M, B](f: L => G[M], g: A => G[B])(implicit G: Semigroupal[G]): (L, A) => G[(M, B)] =
    (l, a) => G.product(f(l), g(a))
}

trait WriterTStreamConcatOps[F[_], L, A] extends WriterConcatOps {
  def stream: WriterT[F, L, A]

  def mapConcat[B](
    f: A => immutable.Iterable[B],
    f2: ((L, immutable.Iterable[B])) => immutable.Iterable[(L, B)]
  )(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, B] =
    WriterT(stream.run.mapConcat { case (l, a) => f2(l -> f(a)) })

  def safeMapConcat[B](
    f: A => immutable.Iterable[B],
    f2: ((L, immutable.Iterable[Option[B]])) => immutable.Iterable[(L, Option[B])]
  )(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, Option[B]] =
    WriterT(stream.run.mapConcat { case (l, a) => f2(l -> makeSafe(f(a))) })

  def mapConcatHead[B](f: A => immutable.Iterable[B])(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, B] =
    mapConcat[B](f, head[L, B])

  def safeMapConcatHead[B](
    f: A => immutable.Iterable[B]
  )(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, Option[B]] =
    safeMapConcat[B](f, head[L, Option[B]])

  def mapConcatTail[B](f: A => immutable.Iterable[B])(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, B] =
    mapConcat[B](f, tail[L, B])

  def safeMapConcatTail[B](
    f: A => immutable.Iterable[B]
  )(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, Option[B]] =
    safeMapConcat[B](f, tail[L, Option[B]])

  def mapConcatAll[B](f: A => immutable.Iterable[B])(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, B] =
    mapConcat[B](f, all[L, B])

  def safeMapConcatAll[B](
    f: A => immutable.Iterable[B]
  )(implicit F: Stream[F], L: Monoid[L]): WriterT[F, L, Option[B]] =
    safeMapConcat[B](f, all[L, Option[B]])
}

trait WriterConcatOps {
  protected def makeSafe[A](cs: immutable.Iterable[A]): immutable.Iterable[Option[A]] =
    if (cs.isEmpty) Vector(None)
    else cs.map(Some(_))

  def head[S: Monoid, A]: ((S, immutable.Iterable[A])) => immutable.Iterable[(S, A)] = {
    case (state, data) =>
      data match {
        case d: immutable.Seq[A] => headSeq[S, A](state, d)
        case d => headSeq[S, A](state, d.toVector)
      }
  }

  def tail[S: Monoid, A]: ((S, immutable.Iterable[A])) => immutable.Iterable[(S, A)] = {
    case (state, data) =>
      data match {
        case d: immutable.Seq[A] => tailSeq[S, A](state, d)
        case d => tailSeq[S, A](state, d.toVector)
      }
  }

  def all[S: Monoid, A]: ((S, immutable.Iterable[A])) => immutable.Iterable[(S, A)] = {
    case (state, data) =>
      data match {
        case immutable.Seq() => immutable.Seq.empty
        case d: immutable.Seq[A] => allSeq[S, A](state, d)
        case d => allSeq[S, A](state, d.toVector)
      }
  }

  def headSeq[S, A](state: S, as: immutable.Seq[A])(implicit S: Monoid[S]): immutable.Seq[(S, A)] =
    as match {
      case immutable.Seq() => immutable.Seq.empty
      case head +: tail =>
        (state, head) +: tail.map(a => (S.empty, a))
    }

  def tailSeq[S, A](state: S, as: immutable.Seq[A])(implicit S: Monoid[S]): immutable.Seq[(S, A)] =
    as match {
      case immutable.Seq() => immutable.Seq.empty
      case xs :+ last =>
        xs.map(a => (S.empty, a)) :+ (state -> last)
    }

  def allSeq[S, A](state: S, as: immutable.Seq[A]): immutable.Seq[(S, A)] =
    as.map(a => (state, a))
}
