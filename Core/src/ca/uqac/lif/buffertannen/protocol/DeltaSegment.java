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

/**
 * Expresses message data as a difference (delta) to a reference message.
 * A delta segment is aimed at reducing the amount of data that needs to
 * be transmitted over the communication channel, when a message has few
 * changes with respect to the previous one.
 * @author sylvain
 *
 */
public class DeltaSegment extends MessageSegment
{
  
  /**
   * The segment number to which this delta segment is related
   */
  protected int m_deltaToWhat = -1;
  
  @Override
  public int getSize()
  {
    return TYPE_WIDTH + SEQUENCE_WIDTH + SEQUENCE_WIDTH + LENGTH_WIDTH + m_contents.size();
  }
  
  /**
   * Returns the message segment this delta segment refers to.
   * @return The sequence number of the corresponding message segment
   */
  public int getDeltaToWhat()
  {
    return m_deltaToWhat;
  }
  
  /**
   * Sets the message segment this delta segment refers to.
   * @param d The sequence number of the corresponding message segment
   */
  public void setDeltaToWhat(int d)
  {
    m_deltaToWhat = d;
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
      out.addAll(new BitSequence(SEGMENT_DELTA, TYPE_WIDTH));
      // Write sequence number
      out.addAll(new BitSequence(m_sequenceNumber, SEQUENCE_WIDTH));
      // Write length
      out.addAll(new BitSequence(length, LENGTH_WIDTH));
      // Write segment of which this is a delta
      out.addAll(new BitSequence(m_deltaToWhat, SEQUENCE_WIDTH));
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
    if (bs.size() < SEQUENCE_WIDTH)
    {
      throw new ReadException("Cannot read reference segment number");
    }
    data = bs.truncatePrefix(SEQUENCE_WIDTH);
    bits_read += SEQUENCE_WIDTH;
    m_deltaToWhat = data.intValue();
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
    out.append("Segment type: delta\n");
    out.append("Reference number: ").append(m_deltaToWhat).append("\n");
    out.append("Sequence number: ").append(m_sequenceNumber).append("\n");
    return out.toString();
  }


}
