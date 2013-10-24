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

import java.util.LinkedList;
import java.util.Vector;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;

public class Frame extends Vector<Segment>
{
  /**
   * Dummy UID
   */
  private static final long serialVersionUID = 1L;

  /**
   * Protocol version number for this frame
   */
  protected static final int VERSION_NUMBER = 1;
  
  /**
   * The log of 2
   */
  protected static final double LOG_2 = Math.log(2);
  
  /**
   * Whether to pad a frame with zeros to always reach the maximum size
   */
  protected static final boolean PAD_FRAME = true;
  
  /**
   * Number of bits used to encode the frame length.
   * Currently 12 bits are used, giving a frame a
   * maximum length of 4096 bits.
   */
  protected int m_lengthWidth = 12;
  protected int m_maxLength = (int) Math.pow(2, m_lengthWidth);
  
  /**
   * Sets the maximum length for the frame
   * @param length
   */
  protected void setMaxLength(int length)
  {
    m_maxLength = length;
    m_lengthWidth = (int) Math.ceil(Math.log(length) / LOG_2);
  }
  
  public BitSequence toBitSequence()
  {
    BitSequence out = new BitSequence();
    int length = 0;
    LinkedList<BitSequence> sequences = new LinkedList<BitSequence>();
    for (Segment seg : this)
    {
      BitSequence seg_seq = seg.toBitSequence();
      sequences.add(seg_seq);
      length += seg_seq.size();
    }
    length += 4 + m_lengthWidth; // Add the 4 of version number and length 
    if (length > m_maxLength)
    {
      // Data is too long for frame: fail
      return null;
    }
    try
    {
      BitSequence data;
      data = new BitSequence(VERSION_NUMBER, 4);
      out.addAll(data);
      data = new BitSequence(length, m_lengthWidth);
      out.addAll(data);
    }
    catch (BitFormatException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    for (BitSequence bs : sequences)
    {
      out.addAll(bs);
    }
    if (PAD_FRAME)
    {
      // Fill remaining space with 0s
      for (int i = length; i < m_maxLength; i++)
      {
        out.add(false);
      }
    }
    return out;
  }
  
  public void fromBitSequence(BitSequence bs) throws ReadException
  {
    BitSequence data;
    int bits_read = 0;
    if (bs.size() < 4)
    {
      throw new ReadException("Cannot read frame version");
    }
    data = bs.truncatePrefix(4);
    bits_read += 4;
    int version = data.intValue();
    if (version != VERSION_NUMBER)
    {
      throw new ReadException("Incorrect version number");
    }
    if (bs.size() < m_lengthWidth)
    {
      throw new ReadException("Cannot read frame length");
    }
    data = bs.truncatePrefix(m_lengthWidth);
    bits_read += m_lengthWidth;
    int frame_length = data.intValue();
    while (bits_read < frame_length)
    {
      if (bs.size() < Segment.TYPE_WIDTH)
      {
        throw new ReadException("Cannot read segment type");
      }
      data = bs.truncatePrefix(Segment.TYPE_WIDTH);
      bits_read += Segment.TYPE_WIDTH;
      int segment_type = data.intValue();
      if (segment_type == Segment.SEGMENT_BLOB)
      {
        throw new ReadException("Blob segments are currently unsupported");
      }
      else if (segment_type == Segment.SEGMENT_MESSAGE)
      {
        MessageSegment seg = new MessageSegment();
        int read = seg.fromBitSequence(bs);
        this.add(seg);
        bits_read += read;
      }
      else if (segment_type == Segment.SEGMENT_SCHEMA)
      {
        SchemaSegment seg = new SchemaSegment();
        int read = seg.fromBitSequence(bs);
        this.add(seg);
        bits_read += read;
      }
      else if (segment_type == Segment.SEGMENT_DELTA)
      {
        DeltaSegment seg = new DeltaSegment();
        int read = seg.fromBitSequence(bs);
        this.add(seg);
        bits_read += read;
      }
    }
  }
}
