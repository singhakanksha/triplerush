/*
 *  @author Philip Stutz
 *  
 *  Copyright 2014 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package com.signalcollect.triplerush.sparql

import scala.collection.JavaConversions._
import com.hp.hpl.jena.query.QueryFactory
import com.signalcollect.triplerush.TriplePattern

object Sparql {
  // Index -1 gets mapped to index 0, -2 to 1, etc.
  @inline private def idToIndex(id: Int) = math.abs(id) - 1

  def apply(query: String): Sparql = {
    val parsed: ParsedSparqlQuery = SparqlParser.parse(query)
    val prefixes = parsed.prefixes
    val select = parsed.select
    val selectVariableNames = select.selectVariableNames
    val numberOfSelectVariables = selectVariableNames.size
    var selectVariableIds = List.empty[Int]
    var nextVariableId = -1
    var variableNameToId = Map.empty[String, Int]
    var idToVariableName = new Array[String](numberOfSelectVariables)
    for (varName <- selectVariableNames) {
      val id = addVariableEncoding(varName)
      selectVariableIds = id :: selectVariableIds
    }

    def addVariableEncoding(variableName: String): Int = {
      val idOption = variableNameToId.get(variableName)
      if (idOption.isDefined) {
        idOption.get
      } else {
        val id = nextVariableId
        nextVariableId -= 1
        variableNameToId += variableName -> id
        idToVariableName(idToIndex(id)) = variableName
        id
      }
    }

    def dictionaryEncodePatterns(patterns: Seq[ParsedPattern]): Seq[TriplePattern] = {
      def expandPrefix(prefixedString: String): String = {
        val prefixEnd = prefixedString.indexOf(':')
        if (prefixEnd == -1) {
          throw new Exception(s"Iri $prefixedString should have a prefix, but none was found.")
        }
        val prefix = prefixedString.substring(0, prefixEnd)
        try {
          val remainder = prefixedString.substring(prefixEnd + 1)
          val expanded = prefixes(prefix)
          expanded + remainder
        } catch {
          case t: Throwable =>
            throw new Exception(s"""Prefix "$prefix" was not declared or $prefixedString did not properly define a prefix.""")
        }
      }
      
      patterns.map {
        case ParsedPattern(s, p, o) =>
                  
      }
      ???
    }

    //           val tripleComponents = Vector(triple.getSubject, triple.getPredicate, triple.getObject)
    //        val tripleIds = tripleComponents map { e =>
    //          if (e.isVariable) {
    //            val variableName = e.getName
    //            addVariableEncoding(variableName)
    //          } else if (e.isLiteral) {
    //            val literalString: String = e.getLiteral.toString
    //            if (Dictionary.contains(literalString)) {
    //              Dictionary(literalString)
    //            } else {
    //              //println(s"$literalString not in store, no results.")
    //              hasNoResults = true
    //              Int.MaxValue
    //            }
    //          } else {
    //            val uriString = e.getURI
    //            if (Dictionary.contains(uriString)) {
    //              Dictionary(uriString)
    //            } else {
    //              //println(s"$uriString not in store, no results.")
    //              hasNoResults = true
    //              Int.MaxValue
    //            }
    //          }
    //        }
    //        patterns = patterns ::: List(TriplePattern(tripleIds(0), tripleIds(1), tripleIds(2)))

//    Some(QuerySpecification(
//      unmatched = patterns,
//      selectVarIds = Some(selectVarIds),
//      variableNameToId = Some(variableNameToId),
//      idToVariableName = Some(idToVariableName),
//      distinct = jenaQuery.isDistinct))

    Sparql(
      selectVariableIds = selectVariableIds,
      variableNameToId = variableNameToId,
      idToVariableName = idToVariableName,
      isDistinct = parsed.select.isDistinct,
      orderBy = select.orderBy.map(variableNameToId),
      limit = select.limit)
  }
}

/**
 * Class for SPARQL Query executions.
 */
case class Sparql(
  selectVariableIds: List[Int],
  variableNameToId: Map[String, Int],
  idToVariableName: Array[String],
  isDistinct: Boolean = false,
  orderBy: Option[Int],
  limit: Option[Int]) {

  //  def counts(variableId: Int, encodedResults: Traversable[Array[Int]]): Map[Int, Int] = {
  //    var counts = Map.empty[Int, Int].withDefaultValue(0)
  //    for (encodedResult <- encodedResults) {
  //      val binding = encodedResult(math.abs(variableId) - 1)
  //      val countForBinding = counts(binding)
  //      counts += binding -> { countForBinding + 1 }
  //    }
  //    counts
  //  }
  //
  //  def decodeResults(encodedResults: Traversable[Array[Int]]): Option[Traversable[Map[String, String]]] = {
  //    if (variableNameToId.isDefined && idToVariableName.isDefined && selectVarIds.isDefined) {
  //      val parEncodedResults: ParArray[Array[Int]] = encodedResults.toArray.par
  //      val select = selectVarIds.get
  //      val varToId = variableNameToId.get
  //      val idToVar = idToVariableName.get
  //      val variables = varToId.keys
  //      val decodedResultMaps = parEncodedResults.map { encodedResults =>
  //        val numberOfBindings = encodedResults.length
  //        val decodedResultMap = select.map { variableId =>
  //          idToVar(variableId) -> Dictionary(encodedResults(-variableId - 1))
  //        }.toMap
  //        decodedResultMap
  //      }
  //      Some(decodedResultMaps.seq)
  //    } else {
  //      None
  //    }
  //  }

}
