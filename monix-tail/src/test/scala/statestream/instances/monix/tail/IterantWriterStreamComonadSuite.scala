package statestream

import cats.kernel.Monoid
import cats.{~>, Bimonad, Eval, Monad}
import monix.eval.Task
import monix.tail.Iterant

class IterantWriterStreamComonadSuite extends IterantWriterStreamIterantSuite[Eval] {

  override implicit def G: Monad[Eval] = implicitly[Bimonad[Eval]]
  override implicit def nat: ~>[Eval, Task] = new ~>[Eval, Task] {
    override def apply[A](fa: Eval[A]): Task[A] = Task.fromEval(fa)
  }

  override def mkWriterStream[S: Monoid, A](
    src: Iterant[Task, A]
  ): WriterStream[Iterant[Task, ?], Eval, Task, Task, S, A] =
    WriterStreamComonad(src)

  override def mkWriterStream[S, A](
    src: Iterant[Task, (S, A)]
  ): WriterStream[Iterant[Task, ?], Eval, Task, Task, S, A] = WriterStreamComonad(src)

  override def extract[A](fa: Eval[A]): A = fa.value
}
