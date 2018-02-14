package org.ifossz.amw.actor

case class TaskId(id: String) extends AnyVal

case class WorkId(id: String) extends AnyVal

final case class Task[T, R](id: TaskId, execution: Seq[T] => R)

final case class Work[T](id: WorkId, input: Seq[T])
