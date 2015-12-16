package chana.xpath

import chana.avro
import chana.xpath.nodes.XPathParser
import chana.xpath.rats.XPathGrammar
import java.io.StringReader
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericData.Record
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import scala.collection.JavaConversions._
import xtc.tree.Node

class XPathUpdateSpec extends WordSpecLike with Matchers with BeforeAndAfterAll {
  import chana.avro.AvroRecords._

  def parse(query: String) = {
    val reader = new StringReader(query)
    val grammar = new XPathGrammar(reader, "")
    val r = grammar.pXPath(0)
    val rootNode = r.semanticValue[Node]
    info("\n\n## " + query + " ##")

    // now let's do Parsing
    val parser = new XPathParser()
    val stmt = parser.parse(query)
    info("\nParsed:\n" + stmt)
    stmt
  }

  def select(record: Record, query: String) = {
    val e = new XPathEvaluator()
    val stmt = parse(query)
    val res = e.select(record, stmt) map (_.value)
    info("\nSelect:\n" + res)
    res
  }

  def update(record: Record, query: String, value: Any) = {
    val e = new XPathEvaluator()
    val stmt = parse(query)
    val res = e.update(record, stmt, value)
    info("\nUpdate:\n" + res)
    res foreach { _.commit() }
  }

  def insertJson(record: Record, query: String, value: String) = {
    val e = new XPathEvaluator()
    val stmt = parse(query)
    val res = e.insertJson(record, stmt, value)
    info("\nUpdate:\n" + res)
    res foreach { _.commit() }
  }

  def insertAllJson(record: Record, query: String, value: String) = {
    val e = new XPathEvaluator()
    val stmt = parse(query)
    val res = e.insertAllJson(record, stmt, value)
    info("\nUpdate:\n" + res)
    res foreach { _.commit() }
  }

  "XPath Update" when {

    "update fields" should {
      val record = initAccount()
      record.put("registerTime", 10000L)
      record.put("lastLoginTime", 20000L)
      record.put("id", "abcd")

      var q = "/registerTime"
      update(record, q, 8)
      select(record, q).head should be(
        8)

      q = "/lastChargeRecord/time"
      update(record, q, 8)
      select(record, q).head should be(
        8)

      q = "/devApps/numBlackApps"
      update(record, q, 88)
      select(record, q).head should be(
        List(88, 88, 88))

      q = "/devApps/@*/numBlackApps"
      update(record, q, 888)
      select(record, q).head should be(
        List(888, 888, 888))

      q = "/devApps/@a/numBlackApps"
      update(record, q, 8)
      select(record, q).head should be(
        8)

      q = "/chargeRecords[2]/time"
      update(record, q, 88)
      select(record, q).head should be(
        List(88))

      q = "/chargeRecords[position()>0]/time"
      update(record, q, 888)
      select(record, q).head should be(
        List(888, 888))

      q = "/chargeRecords/time"
      update(record, q, 8888)
      select(record, q).head should be(
        List(8888, 8888))

    }

    "insert fields" should {
      val record = initAccount()
      record.put("registerTime", 10000L)
      record.put("lastLoginTime", 20000L)
      record.put("id", "abcd")

      var q = "/devApps"
      var json = "{'e' : {}}"
      var value = avro.FromJson.fromJsonString("{}", appInfoSchema, false)
      insertJson(record, q, json)
      select(record, q).head.asInstanceOf[java.util.Map[String, _]].get("e") should be(
        value)

      q = "/chargeRecords"
      json = "{'time': 4, 'amount': -4.0}"
      value = avro.FromJson.fromJsonString(json, chargeRecordSchema, false)
      insertJson(record, q, json)
      select(record, q).head.asInstanceOf[java.util.Collection[_]].contains(value) should be(
        true)

    }

    "insertAll fields" should {
      val record = initAccount()
      record.put("registerTime", 10000L)
      record.put("lastLoginTime", 20000L)
      record.put("id", "abcd")

      var q = "/devApps"
      var json = "{'g' : {}, 'h' : {'numBlackApps': 10}}"
      var value = avro.FromJson.fromJsonString("{'numBlackApps': 10}", appInfoSchema, false)
      insertAllJson(record, q, json)
      select(record, q).head.asInstanceOf[java.util.Map[String, _]].get("h") should be(
        value)

      q = "/chargeRecords"
      json = "[{'time': 3, 'amount': -5.0}, {'time': 4, 'amount': -6.0}]"
      value = avro.FromJson.fromJsonString(json, chargeRecordsSchema, false)
      insertAllJson(record, q, json)
      select(record, q).head.asInstanceOf[java.util.Collection[_]].containsAll(value.asInstanceOf[java.util.Collection[_]]) should be(
        true)

    }
  }

}
