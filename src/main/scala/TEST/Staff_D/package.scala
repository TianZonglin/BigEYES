/*
 * Copyright 2015 Matteo Ceccarello
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package TEST

/**
 * This package provides functions to approximate the diameter of large graphs.
 * The main entry point to the library is the [[TEST.Staff_D.DiameterApproximation]] object
 */
package object Staff_D {

  /**
   * Alias for distances.
   */
  type Distance = Double

  /**
   * Represents an infinite distance.
   */
  val Infinity = Double.PositiveInfinity

}
