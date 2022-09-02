package doric
package syntax

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.language.dynamics

import _root_.doric.types.SparkType
import doric.sem.Location

trait MacroHelpers {
  val c: Context
  import c.universe._

  protected def error(msg: String) = c.error(c.enclosingPosition, msg)

}

class DStructMacros(val c: Context) extends MacroHelpers {
  import c.universe._

  private def lookup(
      column: Tree,
      key: Tree,
      TType: Type,
      AType: Type /*,
      STAtree: Tree,
      Ltree: Tree*/
  ) =
    q"new DStructOps[$TType]($column).getChild[$AType]($key)(implicitly, implicitly)" // /*($STAtree, $Ltree)*/
  // q"new DStructOps[$TType]($column).getChild[Int]($key)(implicitly, implicitly)" // /*($STAtree, $Ltree)*/
  // q"new DStructOps[$TType]($column)"
  // q"""col[Int]("name")"""

  def lookupMacro[A: c.WeakTypeTag, T: c.WeakTypeTag](
      name: c.Expr[String]
  ): c.Expr[DoricColumn[A]] = {
    val Ttpe: Type = implicitly[c.WeakTypeTag[T]].tpe
    val Atpe: Type = implicitly[c.WeakTypeTag[A]].tpe
    // val STAtpe: Type = c.universe.appliedType(typeOf[SparkType[_]], Atpe)
    // val STAtree: Tree = c.inferImplicitValue(STAtpe)
    // val Ltree: Tree   = c.inferImplicitValue(typeOf[Location])
    println(s"T: $Ttpe")
    println(s"A: $Atpe")
    println(s"K: $name")
    // println(s"SparkType[A]: $STAtree")
    val t =
      c.Expr[DoricColumn[A]](
        lookup(c.prefix.tree, name.tree, Ttpe, Atpe /*, STAtree, Ltree*/ )
      )
    println(s"RESULT: $t")
    t
  }

  def lookupMacroApply[A: c.WeakTypeTag, T: c.WeakTypeTag](
      name: c.Expr[String]
  )(): c.Expr[DoricColumn[A]] = {
    val Ttpe: Type = implicitly[c.WeakTypeTag[T]].tpe
    val Atpe: Type = implicitly[c.WeakTypeTag[A]].tpe
    // val STAtpe: Type = c.universe.appliedType(typeOf[SparkType[_]], Atpe)
    // val STAtree: Tree = c.inferImplicitValue(STAtpe)
    // val Ltree: Tree   = c.inferImplicitValue(typeOf[Location])
    println(s"T: $Ttpe")
    println(s"A: $Atpe")
    println(s"K: $name")
    // println(s"SparkType[A]: $STAtree")
    val t =
      c.Expr[DoricColumn[A]](
        lookup(c.prefix.tree, name.tree, Ttpe, Atpe /*, STAtree, Ltree*/ )
      )
    println(s"\nRESULT: $t")
    t
  }

}
