package doric
package syntax

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.language.dynamics

import _root_.doric.types.SparkType
import doric.sem.Location

import org.apache.spark.sql.Row

class DStructMacros(val c: Context) {
  import c.universe._

  private def genColumn(
      column: Tree,
      key: Tree,
      TType: Type,
      AType: Type /*,
      STAtree: Tree,
      Ltree: Tree*/
  ) = {
    val STAtree: Tree =
      c.inferImplicitValue(appliedType(typeOf[SparkType[_]], AType))
    val Ltree: Tree = c.inferImplicitValue(typeOf[Location])
    q"new DStructOps[$TType]($column).getChild[$AType]($key)($STAtree, $Ltree): DoricColumn[$AType]" // /*($STAtree, $Ltree)*/
  }

  def inferAFromCaseClass(parent: Type, member: String): Option[Type] = {
    val result = parent.member(TermName(member)).infoIn(parent).resultType
    if (result == NoType) None
    else Some(result)
  }
  /*
  def columnMacroSelect[A: c.WeakTypeTag, T: c.WeakTypeTag](
      name: c.Expr[String]
  ): Tree =
    columnMacroApply(name)()
   */
  def columnMacroApply[A: c.WeakTypeTag, T: c.WeakTypeTag](
      name: c.Expr[String]
  )(): Tree = {

    val Ttpe: Type = implicitly[c.WeakTypeTag[T]].tpe
    val Atpe: Type = implicitly[c.WeakTypeTag[A]].tpe

    val TisRow       = Ttpe =:= typeOf[Row]
    val TisCaseClass = Ttpe.typeSymbol.asInstanceOf[ClassSymbol].isCaseClass

    val AisSpecified                       = !(Atpe =:= typeOf[Nothing])
    val Literal(Constant(nameStr: String)) = name.tree
    val keyType: Option[Type] =
      if (!TisCaseClass) None else inferAFromCaseClass(Ttpe, nameStr)
    val matchATypes = !AisSpecified || keyType.fold(true) { keyType =>
      keyType =:= Atpe
    }

    println(s"T: $Ttpe, isRow: $TisRow, isCaseClass: $TisCaseClass")
    println(s"K: $nameStr")
    println(
      s"A: $Atpe, isSpecified: $AisSpecified, inferred: $keyType, match: $matchATypes"
    )

    if (!TisRow && !TisCaseClass) {
      c.error(
        c.enclosingPosition,
        s"Enclosing type $Ttpe is neither a case class nor `Row`"
      )
      EmptyTree
    } else if (TisRow && !AisSpecified) {
      c.error(
        c.enclosingPosition,
        "Type of member field not specified in a dynamic context (i.e. Row)"
      )
      EmptyTree
    } else if (TisRow && AisSpecified)
      genColumn(c.prefix.tree, name.tree, Ttpe, Atpe)
    else if (!keyType.isDefined) {
      c.error(
        c.enclosingPosition,
        s"Field `$nameStr` is not a member of case class $Ttpe"
      )
      EmptyTree
    } else if (!matchATypes) {
      c.error(
        c.enclosingPosition,
        s"Type ${keyType.head} of field `$nameStr` in case class $Ttpe differs from specified type $Atpe"
      )
      EmptyTree
    } else
      genColumn(c.prefix.tree, name.tree, Ttpe, keyType.head)
  }

}
