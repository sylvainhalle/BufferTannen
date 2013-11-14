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

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;

public class BlobSegment extends Segment
{
  public static final int LENGTH_WIDTH = 12;
  public static final int MAX_LENGTH = (int) Math.pow(2, LENGTH_WIDTH);
  
  /**
   * The binary contents of the blob segment
   */
  protected BitSequence m_contents;

  @Override
  public BitSequence toBitSequence()
  {
    BitSequence out = new BitSequence();
    int length = m_contents.size();
    if (length > MAX_LENGTH)
    {
      // Contents too long for maximum segment length: fail
      return null;
    }
    if (m_sequenceNumber < 0 || m_sequenceNumber > MAX_SEQUENCE)
    {
      // Sequence number outside of range: fail
      return null;
    }
    try
    {
      // Write segment type number
      out.addAll(new BitSequence(SEGMENT_BLOB, TYPE_WIDTH));
      // Write sequence number
      out.addAll(new BitSequence(m_sequenceNumber, SEQUENCE_WIDTH));
      // Write length
      out.addAll(new BitSequence(length, LENGTH_WIDTH));
      // Write contents
      out.addAll(m_contents);
    }
    catch (BitFormatException e)
    {
      // Do nothing
    }
    return out;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    BitSequence data;
    int bits_read = 0;
    // Segment type number was already consumed by the frame reading method,
    // so we don't need to process it here
    // Read sequence number
    if (bs.size() < SEQUENCE_WIDTH)
    {
      throw new ReadException("Cannot read segment sequence number");
    }
    data = bs.truncatePrefix(SEQUENCE_WIDTH);
    bits_read += SEQUENCE_WIDTH;
    m_sequenceNumber = data.intValue();
    // Read length
    if (bs.size() < LENGTH_WIDTH)
    {
      throw new ReadException("Cannot read segment length");
    }
    data = bs.truncatePrefix(LENGTH_WIDTH);
    bits_read += LENGTH_WIDTH;
    int length = data.intValue();
    // Read contents
    if (bs.size() < length)
    {
      throw new ReadException("Bit sequence shorter than segment declared length");
    }
    m_contents = bs.truncatePrefix(length);
    bits_read += length;
    return bits_read;
  }
  
  @Override
  public String toString()
  {
    StringBuilder out = new StringBuilder();
    out.append("Segment type: blob\n");
    out.append("Sequence number: ").append(m_sequenceNumber).append("\n");
    return out.toString();
  }
  
  public static int getHeaderSize()
  {
    return TYPE_WIDTH + SEQUENCE_WIDTH + LENGTH_WIDTH; 
  }

  @Override
  public int getSize()
  {
    return getHeaderSize() + m_contents.size();
  }
  
  /**
   * Sets the binary contents representing the message carried by
   * the segment
   * @param contents A bit sequence containing the message
   */
  public void setContents(BitSequence contents)
  {
    m_contents = contents;
  }
  
  /**
   * Gets the binary contents representing the message carried by
   * the segment
   * return A bit sequence containing the message
   */
  public BitSequence getContents()
  {
    return m_contents;
  }

}
