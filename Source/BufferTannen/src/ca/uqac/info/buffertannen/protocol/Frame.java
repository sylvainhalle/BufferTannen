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
   * Number of bits used to encode the frame length.
   * Currently 12 bits are used, giving a frame a
   * maximum length of 4096 bits.
   */
  protected static final int LENGTH_WIDTH = 12;
  protected static final int MAX_LENGTH = (int) Math.pow(2, LENGTH_WIDTH);
  
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
    if (length > MAX_LENGTH)
    {
      // Data is too long for frame: fail
      return null;
    }
    try
    {
      BitSequence data;
      data = new BitSequence(VERSION_NUMBER, 4);
      out.addAll(data);
      data = new BitSequence(length, LENGTH_WIDTH);
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
    if (bs.size() < LENGTH_WIDTH)
    {
      throw new ReadException("Cannot read frame length");
    }
    data = bs.truncatePrefix(LENGTH_WIDTH);
    bits_read += LENGTH_WIDTH;
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
    }
  }
}
