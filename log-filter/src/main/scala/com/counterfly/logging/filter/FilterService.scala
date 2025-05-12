package com.counterfly.logging.filter

trait FilterService {
  def terminalWords(logMessages: Seq[LogMessage]): Seq[FilterService.TerminalWord]
}

object FilterService {
  case class TerminalWord(word: String, count: Int)

  def apply() = new FilterService {
    override def terminalWords(logMessages: Seq[LogMessage]): Seq[TerminalWord] = {
      val trie = new MessageTrie()

      // Insert all log messages into the trie
      logMessages.foreach { logMessage =>
        trie.insert(logMessage.message)
      }

      // Collect terminal words from the trie
      trie.collectTerminalWords()
    }
  }
}

class MessageTrie {
  import MessageTrie._

  val root: TrieNode = TrieNode()

  /**
   * Insert a word into the trie and increment counts along the path
   */
  def insert(word: String): Unit = {
    var current = root

    word.toLowerCase.foreach { char =>
      if (!current.children.contains(char)) {
        current.children(char) = TrieNode(Some(char))
      }
      current = current.children(char)
      current.count += 1
    }
  }

  /**
   * Collects all nodes that are terminal
   * Terminal nodes are leaves and nodes whose count are higher than:
   * 1. The sum of all child nodes's counts +
   * 2. The number of child nodes
   */
  def collectTerminalWords(): Seq[FilterService.TerminalWord] = collectTerminals(root, "", Int.MaxValue)

  /**
   * Collect words and their counts recursively from a given node
   */
  private def collectTerminals(node: TrieNode, prefix: String, limit: Int): Seq[FilterService.TerminalWord] = {
    val nodeWord = node.char.map(c => prefix + c).getOrElse(prefix)
    val results = scala.collection.mutable.ListBuffer[FilterService.TerminalWord]()

    // Recursively collect words from children
    for ((char, childNode) <- node.children) {
      if (results.size < limit) {
        results ++= collectTerminals(childNode, nodeWord, limit - results.size)
      }
    }

    // This will cause a post-order traversal doing this operation from the bottom up
    // `results` has the number of terminal words collected below `node`
    // `node` is terminal if:
    //   the number of the occurrences this word has appeared in its sub results (terminal words that it is a prefix of) is less
    //     than the number of occurrences of this word
    val numAppearancesAsSubwordOfTerminalWords = results.map { tw =>
      // distance is a measure of how many characters are missing between `nodeWord` and `tw.word`
      val distance = tw.word.length - nodeWord.length
      tw.count * (distance + 1)
    }.sum
    if (node.count > numAppearancesAsSubwordOfTerminalWords) {
      results += FilterService.TerminalWord(nodeWord, node.count - numAppearancesAsSubwordOfTerminalWords)
    }

    results.toSeq
  }

  /**
   * Return a string representation of the trie for debugging
   */
  // def debugPrint(): String = {
  //   val sb = new StringBuilder()
  //   printNode(root, "", sb)
  //   sb.toString
  // }

  // private def printNode(node: TrieNode, prefix: String, sb: StringBuilder): Unit = {
  //   val nodeStr = node.char.map(_.toString).getOrElse("root")
  //   sb.append(s"$prefix$nodeStr (${node.count})\n")

  //   for ((char, child) <- node.children) {
  //     printNode(child, prefix + "  ", sb)
  //   }
  // }
}

object MessageTrie {
  /**
   * Trie data structure with node counts for efficient prefix-based search
   */
  case class TrieNode(
    char: Option[Char] = None,
    var count: Int = 0,
    children: scala.collection.mutable.Map[Char, TrieNode] = scala.collection.mutable.Map.empty,
  ) {

    // Helper function for local testing
    def debug: String = {
      val charStr = char.map(_.toString).getOrElse("root")
      s"TrieNode(char=$charStr, count=$count)"
    }
  }
}
