package otters.syntax.monix.tail

import cats.data.EitherT
import monix.eval.Task
import otters.syntax.{EitherTApply, EitherTApplyEither, EitherTExtendedSyntax, EitherTSyntax}
import otters.{FunctionPipe, FunctionSink}

trait IterantTaskEitherTSyntax
    extends EitherTSyntax
    with EitherTExtendedSyntax[FunctionPipe[IterantTask, ?, ?], FunctionSink[IterantTask, Task, ?, ?]] {
  implicit class EitherTPipeOps[A, B, C, D](
    override val stream: EitherT[FunctionPipe[IterantTask, Either[A, B], ?], C, D]
  ) extends AllOps[FunctionPipe[IterantTask, Either[A, B], ?], C, D]

  implicit class EitherTFlowApply[A, B](override val stream: FunctionPipe[IterantTask, A, B])
      extends EitherTApply[FunctionPipe[IterantTask, A, ?], B]

  implicit class EitherTFlowApplyEither[A, B, C](override val stream: FunctionPipe[IterantTask, A, Either[B, C]])
      extends EitherTApplyEither[FunctionPipe[IterantTask, A, ?], B, C]
}
