package ci.release.early

import scala.jdk.CollectionConverters.*

private[early] object CollectionConversions:
  def toScala[A](jl: java.util.List[A]): Seq[A] = jl.asScala.toSeq
