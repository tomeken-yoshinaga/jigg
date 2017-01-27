package jigg.ml.keras

/*
 Copyright 2013-2015 Hiroshi Noji
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
     http://www.apache.org/licencses/LICENSE-2.0
     
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitation under the License.
*/

import breeze.linalg.argmax
import jigg.ml.keras._
import jigg.util.LookupTable

import scala.collection.mutable.ListBuffer

class KerasParser(modelPath: String, tablePath: String) {

  private val model = KerasModel(modelPath)
  private val table = LookupTable(tablePath)

  /*
 * BIO tag
 *  B : Begin of segment.               Value is 0.
 *  I : Continuation or end of segment. Value is 1.
 *  O : Outside of segment.             Value is 2.
 */
  private val tagset:Map[Int, String] = Map(0 -> "B", 1 -> "I", 2 -> "O")

  def parsing(str: String): List[(Int, Int)] = {
    // For dummy input to indicate boundaries of sentence.
    val s = "\n" + str + "\n"
    val inputData = table.encode(s)
    val outputData = model.convert(inputData)

    val tags = for {
      i <- 1 until outputData.rows - 1
      maxID = argmax(outputData(i, ::))
    } yield maxID

    getOffsets(tags.toList)
  }

  def getOffsets(data: List[Int]): List[(Int, Int)]= {
    val ranges = ListBuffer[(Int, Int)]()
    var bpos = 0

    for(i <- data.indices){
      tagset(data(i)) match{
        case "B" =>
          if(bpos != 0)
            ranges += ((bpos, i))
          bpos = i
        case "O" =>
          if (bpos != 0)
            ranges += ((bpos, i))
          bpos = 0
        case _ if i == data.indices.last =>
          ranges += ((bpos, i + 1))
        case _ =>
      }
    }
    ranges.toList
  }
}

object KerasParser{
  def apply(modelPath: String, tablePath: String) = new KerasParser(modelPath, tablePath)
}
