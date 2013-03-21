package com.signalcollect.triplerush

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import SparqlDsl._
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.util.FileManager
import com.hp.hpl.jena.rdf.model.Model
import com.signalcollect.triplerush.SparqlDsl._
import scala.io.Source
import scala.io.Codec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable.SortedMap
import scala.collection.immutable.TreeMap
import org.specs2.matcher.MatchResult

@RunWith(classOf[JUnitRunner])
class GroundTruthSpec extends SpecificationWithJUnit {

  sequential

  val enabledQueries = Set(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
  val dslEnabled = true
  val sparqlEnabled = false

  val ub = "http://swat.cse.lehigh.edu/onto/univ-bench.owl"
  val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns"

  val dslQueries = List(
    // Query 1
    SELECT ? "X" WHERE (
      | - "X" - s"$ub#takesCourse" - "http://www.Department0.University0.edu/GraduateCourse0",
      | - "X" - s"$rdf#type" - s"$ub#GraduateStudent"),
    // Query 2
    SELECT ? "X" ? "Y" ? "Z" WHERE (
      | - "X" - s"$rdf#type" - s"$ub#GraduateStudent",
      | - "X" - s"$ub#memberOf" - "Z",
      | - "Z" - s"$rdf#type" - s"$ub#Department",
      | - "Z" - s"$ub#subOrganizationOf" - "Y",
      | - "X" - s"$ub#undergraduateDegreeFrom" - "Y",
      | - "Y" - s"$rdf#type" - s"$ub#University"),
    // Query 3
    SELECT ? "X" WHERE (
      | - "X" - s"$ub#publicationAuthor" - "http://www.Department0.University0.edu/AssistantProfessor0",
      | - "X" - s"$rdf#type" - s"$ub#Publication"),
    // Query 4
    SELECT ? "X" ? "Y1" ? "Y2" ? "Y3" WHERE (
      | - "X" - s"$ub#worksFor" - "http://www.Department0.University0.edu",
      | - "X" - s"$rdf#type" - s"$ub#Professor",
      | - "X" - s"$ub#name" - "Y1",
      | - "X" - s"$ub#emailAddress" - "Y2",
      | - "X" - s"$ub#telephone" - "Y3"),
    //Query 5
    SELECT ? "X" WHERE (
      | - "X" - s"$ub#memberOf" - "http://www.Department0.University0.edu",
      | - "X" - s"$rdf#type" - s"$ub#Person"),
    //Query 6
    SELECT ? "X" WHERE (
      | - "X" - s"$rdf#type" - s"$ub#Student"),
    //Query 7
    SELECT ? "X" ? "Y" WHERE (
      | - "http://www.Department0.University0.edu/AssociateProfessor0" - s"$ub#teacherOf" - "Y",
      | - "Y" - s"$rdf#type" - s"$ub#Course",
      | - "X" - s"$ub#takesCourse" - "Y",
      | - "X" - s"$rdf#type" - s"$ub#Student"),
    //Query 8
    SELECT ? "X" ? "Y" ? "Z" WHERE (
      | - "Y" - s"$ub#subOrganizationOf" - "http://www.University0.edu",
      | - "Y" - s"$rdf#type" - s"$ub#Department",
      | - "X" - s"$ub#memberOf" - "Y",
      | - "X" - s"$rdf#type" - s"$ub#Student",
      | - "X" - s"$ub#emailAddress" - "Z"),
    //Query 9
    SELECT ? "X" ? "Y" ? "Z" WHERE (
      | - "Y" - s"$rdf#type" - s"$ub#Faculty",
      | - "Y" - s"$ub#teacherOf" - "Z",
      | - "Z" - s"$rdf#type" - s"$ub#Course",
      | - "X" - s"$ub#advisor" - "Y",
      | - "X" - s"$ub#takesCourse" - "Z",
      | - "X" - s"$rdf#type" - s"$ub#Student"),
    //Query 10
    SELECT ? "X" WHERE (
      | - "X" - s"$ub#takesCourse" - "http://www.Department0.University0.edu/GraduateCourse0",
      | - "X" - s"$rdf#type" - s"$ub#Student"),
    //Query 11
    SELECT ? "X" WHERE (
      | - "X" - s"$ub#subOrganizationOf" - "http://www.University0.edu",
      | - "X" - s"$rdf#type" - s"$ub#ResearchGroup"),
    //Query 12
    SELECT ? "X" ? "Y" WHERE (
      | - "Y" - s"$ub#subOrganizationOf" - "http://www.University0.edu",
      | - "Y" - s"$rdf#type" - s"$ub#Department",
      | - "X" - s"$ub#worksFor" - "Y",
      | - "X" - s"$rdf#type" - s"$ub#Chair"),
    //Query 13
    SELECT ? "X" WHERE (
      | - "http://www.University0.edu" - s"$ub#hasAlumnus" - "X",
      | - "X" - s"$rdf#type" - s"$ub#Person"),
    //Query 14
    SELECT ? "X" WHERE (
      | - "X" - s"$rdf#type" - s"$ub#UndergraduateStudent"))

  val sparqlQueries = List(
    """
# Query1
# This query bears large input and high selectivity. It queries about just one class and
# one property and does not assume any hierarchy information or inference.
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>
SELECT ?X	
WHERE
{?X rdf:type ub:GraduateStudent .
  ?X ub:takesCourse "http://www.Department0.University0.edu/GraduateCourse0"}
""",
    """
# Query2
# This query increases in complexity: 3 classes and 3 properties are involved. Additionally, 
# there is a triangular pattern of relationships between the objects involved.
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>
SELECT ?X ?Y ?Z
WHERE
{?X rdf:type ub:GraduateStudent .
  ?Y rdf:type ub:University .
  ?Z rdf:type ub:Department .
  ?X ub:memberOf ?Z .
  ?Z ub:subOrganizationOf ?Y .
  ?X ub:undergraduateDegreeFrom ?Y}
""",
    """
# Query3
# This query is similar to Query 1 but class Publication has a wide hierarchy.
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>
SELECT ?X
WHERE
{?X rdf:type ub:Publication .
  ?X ub:publicationAuthor 
        <http://www.Department0.University0.edu/AssistantProfessor0>}
""",
    """
# Query4
# This query has small input and high selectivity. It assumes subClassOf relationship 
# between Professor and its subclasses. Class Professor has a wide hierarchy. Another 
# feature is that it queries about multiple properties of a single class.
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#>
SELECT ?X ?Y1 ?Y2 ?Y3
WHERE
{
  ?X ub:worksFor <http://www.Department0.University0.edu> .
        ?X rdf:type ub:Professor .
  ?X ub:name ?Y1 .
  ?X ub:emailAddress ?Y2 .
  ?X ub:telephone ?Y3}
""")

  "LUBM Query 1" should {
    val queryId = 1
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 2" should {
    val queryId = 2
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 3" should {
    val queryId = 3
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 4" should {
    val queryId = 4
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 5" should {
    val queryId = 5
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 6" should {
    val queryId = 6
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 7" should {
    val queryId = 7
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 8" should {
    val queryId = 8
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 9" should {
    val queryId = 9
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 10" should {
    val queryId = 10
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 11" should {
    val queryId = 11
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 12" should {
    val queryId = 12
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 13" should {
    val queryId = 13
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }
  "LUBM Query 14" should {
    val queryId = 14
    s"DSL-match the reference results $queryId" in {
      runTest(queryId, sparql = false)
    }
    s"SPARQL-match the reference results $queryId" in {
      runTest(queryId, sparql = true)
    }
  }

  def toQuery(s: String): PatternQuery = PatternQuery.build(s) match {
    case Left(q) => q
    case Right(error) => throw new Exception(error)
  }

  val qe = new QueryEngine

  for (fileNumber <- 0 to 14) {
    val filename = s"./lubm/university0_$fileNumber.nt"
    print(s"loading $filename ...")
    qe.load(filename)
    println(" done")
  }

  def executeOnQueryEngine(q: PatternQuery): List[Bindings] = {
    val resultFuture = qe.executeQuery(q)
    val result = Await.result(resultFuture, new FiniteDuration(100, TimeUnit.SECONDS))
    val bindings = result map (_.bindings.map map (entry => (Mapping.getString(entry._1), Mapping.getString(entry._2))))
    val sortedBindings = bindings map (unsortedBindings => TreeMap(unsortedBindings.toArray: _*))
    val sortedBindingList = sortedBindings sortBy (map => map.values)
    sortedBindingList
  }

  type Bindings = TreeMap[String, String]
  type QuerySolution = List[Bindings]

  def runTest(queryId: Int, sparql: Boolean = false): MatchResult[Any] = {
    if (enabledQueries.contains(queryId) && (dslEnabled && !sparql || sparqlEnabled && sparql)) {
      val referenceResult = referenceResults(queryId)
      val query: PatternQuery = {
        if (sparql) {
          println(s"Query $queryId SPARQL")
          toQuery(sparqlQueries(queryId - 1))
        } else {
          println(s"Query $queryId DSL")
          dslQueries(queryId - 1)
        }
      }
      val ourResult = executeOnQueryEngine(query)
      //ourResult.length === referenceResult.length
      //ourResult.slice(0, math.min(300, ourResult.length)) === referenceResult.slice(0, math.min(300, referenceResult.length))
      ourResult === referenceResult
    } else {
      "Test was not enabled" === "Test was not enabled"
    }
  }

  val queryBaseName = "./answers/answers_query"
  val referenceFiles: Map[Int, String] = ((1 to 14) map (queryNumber => queryNumber -> (queryBaseName + queryNumber + ".txt"))).toMap
  val referenceResults: Map[Int, QuerySolution] = {
    referenceFiles map { entry =>
      val fileName = entry._2
      val file = Source.fromFile(fileName)
      val lines = file.getLines
      val bindings = getBindings(lines)
      (entry._1, bindings)
    }
  }

  def getBindings(lines: Iterator[String]): QuerySolution = {
    var currentLine = lines.next
    if (currentLine == "NO ANSWERS.") {
      // No bindings.
      List()
    } else {
      val variables = currentLine.split("\t").toIndexedSeq
      var solution = List[Bindings]()
      while (lines.hasNext) {
        var binding = TreeMap[String, String]()
        currentLine = lines.next
        val values = currentLine.split("\t").toIndexedSeq
        for (i <- 0 until variables.size) {
          binding += variables(i) -> values(i)
        }
        solution = binding :: solution
      }
      solution.sortBy(map => map.values)
    }
  }

  //  val debug = SELECT ? "X" ? "Y1" ? "Y2" ? "Y3" WHERE (
  //    | - "X" - s"$ub#telephone" - "Y3",
  //    | - "X" - s"$ub#emailAddress" - "Y2",
  //    | - "X" - s"$ub#name" - "Y1",
  //    | - "X" - s"$rdf#type" - s"$ub#Professor",
  //    | - "X" - s"$ub#worksFor" - "http://www.Department0.University0.edu")

}