package com.counterfly.logging.filter

import FilterService.TerminalWord
import java.time.Instant
import org.specs2.mutable.Specification

class FilterServiceSpec extends Specification {
  "FilterService" should {
    def svc() = FilterService()

    "terminalWords" should {
      "identify terminal words" in {
        // Create sample log messages
        val now = Instant.now()
        val messages = LogMessageUtils.generateLogMessages("castle") ++
          LogMessageUtils.generateLogMessages("cast") ++
          LogMessageUtils.generateLogMessages("fun with spaces") ++
          LogMessageUtils.generateLogMessages("castle") ++
          LogMessageUtils.generateLogMessages("cash") ++
          LogMessageUtils.generateLogMessages("c")

        // Get terminal words
        val service = svc()
        val terminalWords = service.terminalWords(messages)

        // Assert the terminal words are correct
        terminalWords must haveSize(5)
        terminalWords.map(_.word).toSet must contain(allOf("fun with spaces", "castle", "cast", "cash", "c"))

        // Assert the counts for each terminal word
        terminalWords.find(_.word == "c").map(_.count).get must be_>=(1)
        terminalWords.find(_.word == "cash").map(_.count).get must be_>=(1)
        terminalWords.find(_.word == "cast").map(_.count).get must be_>=(1)
        terminalWords.find(_.word == "castle").map(_.count).get must beEqualTo(2)
        terminalWords.find(_.word == "fun with spaces").map(_.count).get must beEqualTo(1)
      }

      "handle empty input" in {
        val emptyMessages = Seq.empty[LogMessage]
        val result = svc().terminalWords(emptyMessages)
        result must beEmpty
      }

      "handle special characters properly" in {
        val now = Instant.now()
        val messages = Seq(
          LogMessage("1", now, "confuse |me|"),
          LogMessage("2", now, "confuse !me]"),
          LogMessage("3", now, "confuse (me>"),
        )

        val terminalWords = svc().terminalWords(messages)

        // Assert the terminal words are correct
        terminalWords must haveSize(3)
        terminalWords.map(_.word).toSet must contain(allOf("confuse |me|", "confuse !me]", "confuse (me>"))

        // Assert the counts for each terminal word
        terminalWords.find(_.word == "confuse |me|").map(_.count).get must be_>=(1)
        terminalWords.find(_.word == "confuse !me]").map(_.count).get must be_>=(1)
        terminalWords.find(_.word == "confuse (me>").map(_.count).get must be_>=(1)

      }
    }
  }
}
