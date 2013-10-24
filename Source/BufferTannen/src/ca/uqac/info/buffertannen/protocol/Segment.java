/*-------------------------------------------------------------------------
    Buffer Tannen, a binary message protocol
    Copyright (C) 2013  Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 -------------------------------------------------------------------------*/
package ca.uqac.info.buffertannen.protocol;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;

public abstract class Segment
{
  /**
   * Enumerated type indicating which type of segment
   * @author sylvain
   */
  public static final int SEGMENT_BLOB = 0;
  public static final int SEGMENT_MESSAGE = 1;
  public static final int SEGMENT_SCHEMA = 2;
  public static final int SEGMENT_DELTA = 3;
  public static final int TYPE_WIDTH = 2;
  public static final int SEQUENCE_WIDTH = 12;
  public static final int MAX_SEQUENCE = (int) Math.pow(2, SEQUENCE_WIDTH);
  
  /**
   * A sequence number associated to each segment
   */
  protected int m_sequenceNumber;
  
  /**
   * Outputs the segment as a sequence of bits
   * @return The sequence of bits corresponding to that segment
   */
  public abstract BitSequence toBitSequence();
  
  /**
   * Populates a segment from a bit sequence
   * @param bs The bit sequence to read from
   * @return The number of bits read from the bit sequence
   */
  public abstract int fromBitSequence(BitSequence bs) throws ReadException;
  
  /**
   * Assigns a sequence number to a segment
   * @param number The sequence number
   */
  public void setSequenceNumber(int number)
  {
    m_sequenceNumber = number;
  }
  
  /**
   * Returns the sequence number given to the segment
   * @return The sequence number
   */
  public int getSequenceNumber()
  {
    return m_sequenceNumber;
  }
  
  /**
   * Computes the size (in bits) of the segment
   * @return The size of the segment
   */
  public abstract int getSize();
}
