/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
 *  
 *  Copyright 2013 University of Zurich
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

package com.signalcollect.triplerush

import language.implicitConversions
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

/**
 * Represents a SPARQL query.
 */
case class QuerySpecification(
  unmatched: Seq[TriplePattern],
  tickets: Long = Long.MaxValue) {

  def withUnmatchedPatterns(u: Seq[TriplePattern]): QuerySpecification = {
    if(!u.forall(tp => tp.s != 0 && tp.p != 0 && tp.o != 0)){
      throw new Exception("TriplePatterns in queries can only contain variables and constants, never a wildcard.")
    }
    
    copy(unmatched = u)
  }

  override def toString = {
    unmatched.toString
  }

}