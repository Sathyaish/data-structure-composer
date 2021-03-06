package implementationSearcher

import implementationSearcher.ImplLhs._
import implementationSearcher.ImplLibrary.Decls
import shared._

/**
  * Created by buck on 9/10/16.
  */
case class SingleMethodImplSet(options: DominanceFrontier[BoundUnnamedImpl]) {
  def impls = options.items

  def bestImplsForConditions(conditions: ImplPredicateMap): DominanceFrontier[BoundUnnamedImpl] = {
    DominanceFrontier.fromSet(implsWhichMatchConditions(conditions).items)
  }

  def add(impl: BoundUnnamedImpl): SingleMethodImplSet = {
    SingleMethodImplSet(options.add(impl))
  }

  lazy val unboundOptions: DominanceFrontier[UnnamedImpl] = {
    DominanceFrontier.fromSet[UnnamedImpl](options.items.map(_.impl))
  }

  def isOtherImplUseful(impl: UnnamedImpl): Boolean = {
    List(RightStrictlyDominates, NeitherDominates).contains(unboundOptions.partialCompareToItem(impl))
  }

  // More impl conditions means that this function returns something better
  def implsWhichMatchMethodExpr(methodExpr: MethodExpr,
                                scope: UnfreeImplSet,
                                list: ParameterList,
                                decls: Decls): Set[(ImplPredicateMap, Impl.Rhs, BoundUnnamedImpl)] =
    options.items.flatMap({ case b@BoundUnnamedImpl(option, optionSource) =>
      option
        .withName(methodExpr.name)
        .bindToContext(methodExpr, scope, list, decls).map((x) => (x._1, x._2, b))
    })

  // More impl conditions means that this function returns something better
  def implsWhichMatchConditions(implPredicates: ImplPredicateMap): DominanceFrontier[BoundUnnamedImpl] = {
    options.filter(_.impl.compatibleWithConditions(implPredicates))
  }

  def toLongString: String = {
    s"{ " + impls.toList.map((impl) => {
      s"    (${impl.impl.predicates.toNiceString}) <- ${impl.impl.cost}"
    }).mkString("\n") + "  }"
  }

  def sum(other: SingleMethodImplSet): SingleMethodImplSet = {
    SingleMethodImplSet(this.options ++ other.options)
  }


  def partialCompare(other: SingleMethodImplSet): DominanceRelationship = {
    (for {
      x <- this.options.items
      y <- other.options.items
    } yield implicitly[PartialOrdering[BoundUnnamedImpl]].partialCompare(x, y)).reduce(_ infimum _)
  }


  // TODO: I have made the terrible choice to have the impl source here be wrong.
  def product(other: SingleMethodImplSet): SingleMethodImplSet =
    SingleMethodImplSet.fromSet(for {
      x <- this.impls
      y <- other.impls
    } yield {
      val lhs = x.impl.predicates.and(y.impl.predicates)
      val rhs = x.impl.cost + y.impl.cost
      BoundUnnamedImpl(UnnamedImpl(lhs, rhs), EmptyBoundSource)
    })

//  def bestFullyGeneralTime: Option[AffineBigOCombo[MethodName]] = {
//    this.bestImplementationForConditions(ImplPredicateList.empty(impls.head.lhs.parameters.length)).map(_.rhs)
//  }
}

object SingleMethodImplSet {
  def fromSet(set: Set[BoundUnnamedImpl]): SingleMethodImplSet = {
    SingleMethodImplSet(DominanceFrontier.fromSet(set))
  }
}
