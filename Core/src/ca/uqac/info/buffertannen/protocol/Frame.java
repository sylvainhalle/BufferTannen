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
import ca.uqac.info.buffertannen.message.SmallsciiElement;

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
   * The number of bits used to encode the version number
   */
  protected static final int VERSION_WIDTH = 4;
  
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
   * Currently 14 bits are used, giving a frame a
   * maximum length of 16384 bits (2 kb)
   */
  public static final int LENGTH_WIDTH = 14;
  public static final int MAX_LENGTH = (int) Math.pow(2, LENGTH_WIDTH);
  
  /**
   * Maximum length allowed for <em>this</em> frame; different from
   * maximum theoretical length
   */
  public int m_maxLength = MAX_LENGTH;
  
  /**
   * Number of bits used to encode the total number of segments
   */
  protected static final int TOTAL_SEGMENTS_WIDTH = 16;
  
  /**
   * Number of bits used to encode the data stream index
   */
  protected static final int DATASTREAM_INDEX_WIDTH = 16;
  
  /**
   * A unique value to identify each data stream; this field has the same
   * role as the "source port" in the TCP protocol.
   */
  protected int m_dataStreamIndex = 0;
  
  /**
   * A character string representing the resource name contained in this
   * stream of data. Typically, this is used to provide a filename for
   * the data transmitted.
   */
  protected SmallsciiElement m_resourceIdentifier = new SmallsciiElement();
  
  /**
   * The total number of segments contained in this transmission.
   * When set to a nonzero value, indicates that data is transmitted
   * in "lake" mode; when set to 0, data is transmitted in "stream"
   * mode.
   */
  protected int m_totalSegments = 0;
  
  /**
   * Sets the resource identifier for this frame
   * @param identifier The identifier
   */
  public void setResourceIdentifier(String identifier)
  {
    m_resourceIdentifier = new SmallsciiElement(identifier);
  }
  
  public void setMaxLength(int length)
  {
    if (length > MAX_LENGTH)
      length = MAX_LENGTH;
    m_maxLength = length;
  }
  
  /**
   * Retrieves the resource identifier for this frame
   * @return The identifier
   */
  public String getResourceIdentifier()
  {
    String ri = m_resourceIdentifier.toString();
    return ri.replaceAll("\"", "");
  }
  
  /**
   * Sets the data stream index for this frame
   * @param index The index
   */
  public void setDataStreamIndex(int index)
  {
    m_dataStreamIndex = index;
  }
  
  /**
   * Retrieves the total number of segments in this communication
   * @return The number of segments
   */
  public int getTotalSegments()
  {
    return m_totalSegments;
  }
  
  /**
   * Sets the total number of segments in this communication
   * @param index The number of segments
   */
  public void setTotalSegments(int n)
  {
    m_totalSegments = n;
  }
  
  /**
   * Retrieves the data stream index for this frame
   * @return The index
   */
  public int getDataStreamIndex()
  {
    return m_dataStreamIndex;
  }
  
  public int getHeaderSize()
  {
    int size = VERSION_WIDTH + LENGTH_WIDTH + TOTAL_SEGMENTS_WIDTH + DATASTREAM_INDEX_WIDTH;
    // The size in bits of the Smallscii string
    size += m_resourceIdentifier.getSize();
    return size;
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
    length += VERSION_WIDTH + LENGTH_WIDTH; 
    if (length > m_maxLength)
    {
      // Data is too long for frame: fail
      return null;
    }
    try
    {
      BitSequence data;
      data = new BitSequence(VERSION_NUMBER, VERSION_WIDTH);
      out.addAll(data);
      data = new BitSequence(length, LENGTH_WIDTH);
      out.addAll(data);
      data = new BitSequence(m_dataStreamIndex, DATASTREAM_INDEX_WIDTH);
      out.addAll(data);
      data = new BitSequence(m_totalSegments, TOTAL_SEGMENTS_WIDTH);
      out.addAll(data);
      data = m_resourceIdentifier.toBitSequence();
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
    // Read version number
    if (bs.size() < VERSION_WIDTH)
    {
      throw new ReadException("Cannot read frame version");
    }
    data = bs.truncatePrefix(VERSION_WIDTH);
    bits_read += VERSION_WIDTH;
    int version = data.intValue();
    if (version != VERSION_NUMBER)
    {
      throw new ReadException("Incorrect version number");
    }
    // Read frame length
    if (bs.size() < LENGTH_WIDTH)
    {
      throw new ReadException("Cannot read frame length");
    }
    data = bs.truncatePrefix(LENGTH_WIDTH);
    bits_read += LENGTH_WIDTH;
    int frame_length = data.intValue();
    // Read datastream index
    if (bs.size() < DATASTREAM_INDEX_WIDTH)
    {
      throw new ReadException("Cannot read datastream index");
    }
    data = bs.truncatePrefix(DATASTREAM_INDEX_WIDTH);
    bits_read += DATASTREAM_INDEX_WIDTH;
    m_dataStreamIndex = data.intValue();
    // Read total segments
    if (bs.size() < TOTAL_SEGMENTS_WIDTH)
    {
      throw new ReadException("Cannot read total number of segments");
    }
    data = bs.truncatePrefix(TOTAL_SEGMENTS_WIDTH);
    bits_read += TOTAL_SEGMENTS_WIDTH;
    m_totalSegments = data.intValue();
    // Read resource identifier
    SmallsciiElement ri = new SmallsciiElement();
    bits_read += ri.fromBitSequence(bs);
    m_resourceIdentifier = ri;
    // Read segments
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
        BlobSegment seg = new BlobSegment();
        int read = seg.fromBitSequence(bs);
        this.add(seg);
        bits_read += read;
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
