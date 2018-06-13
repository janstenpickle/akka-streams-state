package otters.syntax.fs2

import cats.data.EitherT
import cats.effect.IO
import otters.syntax.{EitherTApply, EitherTApplyEither, EitherTExtendedSyntax, EitherTSyntax}
import otters.{FunctionPipe, FunctionSink}

trait Fs2IOEitherTSyntax
    extends EitherTSyntax
    with EitherTExtendedSyntax[FunctionPipe[StreamIO, ?, ?], FunctionSink[StreamIO, IO, ?, ?]] {
  implicit class EitherTPipeOps[A, B, C, D](override val stream: EitherT[FunctionPipe[StreamIO, Either[A, B], ?], C, D])
      extends AllOps[FunctionPipe[StreamIO, Either[A, B], ?], C, D]

  implicit class EitherTFlowApply[A, B](override val stream: FunctionPipe[StreamIO, A, B])
      extends EitherTApply[FunctionPipe[StreamIO, A, ?], B]

  implicit class EitherTFlowApplyEither[A, B, C](override val stream: FunctionPipe[StreamIO, A, Either[B, C]])
      extends EitherTApplyEither[FunctionPipe[StreamIO, A, ?], B, C]
}
