package org.clulab.concepts.apps

import org.clulab.concepts.{ConceptDiscoverer, ConceptSink, ConceptSource, DiscoveryDocument}
import org.clulab.utils.FileUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.io.{FileOutputStream, PrintStream}

object DiscoverAndRankConceptsApp extends App {
  val start = Calendar.getInstance
  val inputDir = args(0)
  val thresholdFrequency = 10
  val thresholdSimilarity = 0
  val topPick = 10000
  // This goes last, even though not used last, because it is optional.
  val sentenceThresholdOpt = None

  val conceptDiscovery = ConceptDiscoverer.fromConfig()
  val files = FileUtils.findFiles(inputDir, "json")
  val discoveryDocuments = files.zipWithIndex.par.flatMap { case (file, index) =>
//    println(s"$index ${file.getName}")
    val conceptSource = ConceptSource(file)
    val docId = conceptSource.getIdOpt.get
    val scoredSentences = conceptSource.getScoredSentences

    // Things elsewhere seem to require at least some text and scored sentences.
    if (conceptSource.text.nonEmpty && scoredSentences.nonEmpty)
      Some(DiscoveryDocument(docId, scoredSentences))
    else
      None
  }.seq
  val time = Calendar.getInstance
  println(TimeUnit.MILLISECONDS.toSeconds(time.getTimeInMillis() - start.getTimeInMillis()))
  val concepts = conceptDiscovery.discoverMostFrequentConcepts(discoveryDocuments, sentenceThresholdOpt, thresholdFrequency, topPick)
  conceptDiscovery.saveConcepts(concepts, "concepts.txt")
  val time2 = Calendar.getInstance
  println(TimeUnit.MILLISECONDS.toSeconds(time2.getTimeInMillis() - time.getTimeInMillis()))
  println(concepts.size)
  val rankedConcepts = conceptDiscovery.rankConcepts(concepts, thresholdSimilarity)
  val time3 = Calendar.getInstance
  println(TimeUnit.MILLISECONDS.toSeconds(time3.getTimeInMillis() - time2.getTimeInMillis()))
  val conceptSink = new ConceptSink(rankedConcepts)

  Console.withOut(new PrintStream(new FileOutputStream("output_full.json"))){
    conceptSink.printJson()
  }
}
