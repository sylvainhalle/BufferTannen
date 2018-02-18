/*
    Buffer Tannen, a binary message protocol
    Copyright (C) 2013-2018  Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uqac.lif.buffertannen.protocol;

import ca.uqac.lif.buffertannen.message.BitFormatException;
import ca.uqac.lif.buffertannen.message.BitSequence;
import ca.uqac.lif.buffertannen.message.ReadException;

public class MessageSegment extends Segment
{
  
  public static final int LENGTH_WIDTH = 12;
  public static final int MAX_LENGTH = (int) Math.pow(2, LENGTH_WIDTH);
  public static final int SCHEMA_WIDTH = 4;
  
  /**
   * The contents of this message segment. This is stored as a bit sequence,
   * since the schema used to encode the underlying message may not be
   * known when the segment is received.
   */
  protected BitSequence m_contents;
  
  /**
   * The schema number to be used to decode this segment
   */
  protected int m_schemaNumber = -1;
  
  @Override
  public int getSize()
  {
    return TYPE_WIDTH + SEQUENCE_WIDTH + SCHEMA_WIDTH + LENGTH_WIDTH + m_contents.size();
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
  
  /**
   * Sets the schema number associated to the segment's contents
   * @param number The schema number
   */
  public void setSchemaNumber(int number)
  {
    m_schemaNumber = number;
  }
  
  /**
   * Get the schema number associated to the segment's contents
   * @return The schema number
   */
  public int getSchemaNumber()
  {
    return m_schemaNumber;
  }

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
      out.addAll(new BitSequence(SEGMENT_MESSAGE, TYPE_WIDTH));
      // Write sequence number
      out.addAll(new BitSequence(m_sequenceNumber, SEQUENCE_WIDTH));
      // Write length
      out.addAll(new BitSequence(length, LENGTH_WIDTH));
      // Write schema number
      out.addAll(new BitSequence(m_schemaNumber, SCHEMA_WIDTH));
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
    // Read schema number
    if (bs.size() < SCHEMA_WIDTH)
    {
      throw new ReadException("Cannot read schema number");
    }
    data = bs.truncatePrefix(SCHEMA_WIDTH);
    bits_read += SCHEMA_WIDTH;
    m_schemaNumber = data.intValue();
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
    out.append("Segment type: message\n");
    out.append("Schema number: ").append(m_schemaNumber).append("\n");
    out.append("Sequence number: ").append(m_sequenceNumber).append("\n");
    return out.toString();
  }
}
