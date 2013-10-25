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

    You should have Sent a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 -------------------------------------------------------------------------*/
package ca.uqac.info.buffertannen.protocol;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.CannotComputeDeltaException;
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.message.TypeMismatchException;

public class Sender
{
  protected LinkedList<Segment> m_segmentBuffer;
  
  protected LinkedList<Segment> m_segmentToRepeatBuffer;
  
  protected Map<Integer,SchemaElement> m_schemas;
  
  /**
   * The maximum length of a frame, in bits
   */
  protected int m_maxFrameLength = 512; 
  
  /**
   * The number of the last schema sent. This is used when
   * periodically broadcasting schemas, to cycle through all
   * defined schemas.
   */
  protected int m_lastSchemaSent = -1;
  
  /**
   * The interval at which to broadcast message schemas. For example,
   * if set to 2, the sender inserts a schema segment after every 2
   * segments sent. Set to 0 for no periodical broadcast at all.
   * If used, this value should be lower than {@Sender.m_repeatAfterN},
   * so that a schema retransmission always occurs between the time
   * a segment is Sent and the time where it is declared lost.
   */
  protected int m_broadcastSchemasEveryN = 10;
  
  /**
   * The number of consecutive delta-segments that can be sent before
   * transmitting a new message segment.
   */
  protected int m_deltaSegmentInterval = 10;
  
  /**
   * The interval after which to repeat a segment a second time.
   * Set to 0 for no repetition at all. This value must not
   * be greater than {@link Receiver.m_lostInterval}, otherwise the
   * receiver will declare a segment as lost before the sender
   * has had a chance to transmit it again. 
   */
  protected int m_repeatAfterN = 20;
  
  /**
   * A counter to give sequential numbers to segments
   */
  protected int m_sequenceNumber = 0;
  
  /**
   * The number of delta segments sent since the last message segment
   */
  protected int m_deltaSegmentsSentSinceLast = -1;
  
  /**
   * The segment sequence number of the last message sent as a
   * complete message segment.
   */
  protected int m_lastFullMessageSentNumber = -1;
  
  /**
   * A memory of the last message sent as a complete message segment.
   * Delta-segments will be calculated with respect to this reference
   * segment.
   */
  protected SchemaElement m_lastFullMessageSent = null;
  
  /* --- Various statistics about segments Sent --- */

  /**
   * Number of frames sent
   */
  protected int m_framesSent = 0;
  
  /**
   * Number of raw bits sent (including repetitions of segments)
   */
  protected int m_rawBitsSent = 0;
  
  /**
   * Number of bits Sent as schema segments
   */
  protected int m_schemaSegmentBitsSent = 0;
  
  /**
   * Number of bits Sent as message segments
   */
  protected int m_messageSegmentBitsSent = 0;
  
  /**
   * Number of bits Sent as delta segments
   */
  protected int m_deltaSegmentBitsSent = 0;
  
  /**
   * Number of bits Sent in <em>distinct</em> schema segments
   */
  protected int m_schemaSegmentDistinctBitsSent = 0;
  
  /**
   * Number of bits Sent in <em>distinct</em> message segments
   */
  protected int m_messageSegmentDistinctBitsSent = 0;
  
  /**
   * Number of bits Sent in <em>distinct</em> delta segments
   */
  protected int m_deltaSegmentDistinctBitsSent = 0;
  
  /**
   * Number of schema segments Sent
   */
  protected int m_schemaSegmentsSent = 0;
  
  /**
   * Number of message segments Sent
   */
  protected int m_messageSegmentsSent = 0;
  
  /**
   * Number of delta segments Sent
   */
  protected int m_deltaSegmentsSent = 0;
  
  public int getNumberOfMessageSegments()
  {
    return m_messageSegmentsSent;
  }
  
  public int getNumberOfSchemaSegments()
  {
    return m_schemaSegmentsSent;
  }
  
  public int getNumberOfDeltaSegments()
  {
    return m_deltaSegmentsSent;
  }
  
  public int getNumberOfMessageSegmentsBits()
  {
    return m_messageSegmentBitsSent;
  }
  
  public int getNumberOfSchemaSegmentsBits()
  {
    return m_schemaSegmentBitsSent;
  }
  
  public int getNumberOfDeltaSegmentsBits()
  {
    return m_deltaSegmentBitsSent;
  }
  
  public int getNumberOfRawBits()
  {
    return m_rawBitsSent;
  }
  
  public int getNumberOfFrames()
  {
    return m_framesSent;
  }
  
  
  public Sender()
  {
    super();
    m_segmentBuffer = new LinkedList<Segment>();
    m_segmentToRepeatBuffer = new LinkedList<Segment>();
    m_schemas = new HashMap<Integer,SchemaElement>();
  }
  
  /**
   * Sets the maximum length of a frame
   * @param length Length of a frame, in bits
   */
  public void setFrameMaxLength(int length)
  {
    m_maxFrameLength = length;
  }
  
  /**
   * Sets the interval at which message segmentsmust be sent.
   * @param interval Interval at which message segmentsmust be sent.
   *   Set to 0 to disable delta segments completely.
   */
  public void setDeltaSegmentInterval(int interval)
  {
    m_deltaSegmentInterval = interval;
  }
  
  /**
   * Polls the sender's output buffer and returns the first
   * frame of that buffer as a sequence of bits, if any exists 
   * @return The first frame in the buffer, null if there is nothing to send
   */
  public BitSequence pollBitSequence()
  {
    Frame f = pollBuffer();
    if (f == null)
    {
      return null;
    }
    m_framesSent++;
    return f.toBitSequence();
  }
  
  /**
   * Polls the sender's output buffer and returns the first
   * frame of that buffer, if any exists 
   * @return The first frame in the buffer, null if buffer is empty 
   */
  public Frame pollBuffer()
  {
    if (m_segmentBuffer.isEmpty())
    {
      return null;
    }
    // Create a frame by packing as many pending segments as possible
    // within frame size limits
    int total_size = 0;
    Frame f = new Frame();
    while (total_size < m_maxFrameLength && !m_segmentBuffer.isEmpty())
    {
      Segment seg = m_segmentBuffer.getFirst();
      int segment_size = seg.getSize();
      if (segment_size > m_maxFrameLength)
      {
        // Problem: this segment will never fit into a frame!
        System.err.println("ERROR: found a segment larger than max frame size");
      }
      total_size += seg.getSize();
      if (total_size < m_maxFrameLength)
      {
        m_segmentBuffer.removeFirst();
        f.add(seg);
        m_rawBitsSent += segment_size;
      }
    }
    return f;
  }
  
  /**
   * Adds a message with given schema number to the sender's
   * segment buffer
   * @param number The schema number associated to that message
   * @param e The message to send
   */
  public void addMessage(int number, SchemaElement e)
  {
    addMessage(number, e, false);
  }
  
  /**
   * Adds a message with given schema number to the sender's
   * segment buffer
   * @param number The schema number associated to that message
   * @param e The message to send
   * @param force_full Set to true to force the message to be sent
   *   in a full message segment, instead of as a delta segment
   */
  public void addMessage(int number, SchemaElement e, boolean force_full)
  {
    // Create frame with message
    MessageSegment ms = null; 
    if (!(force_full || m_deltaSegmentsSentSinceLast == -1 || m_deltaSegmentsSentSinceLast > m_deltaSegmentInterval || m_lastFullMessageSent == null))
    {
      // We can afford to send a delta-segment instead
      ms = new DeltaSegment();
      ((DeltaSegment) ms).setDeltaToWhat(m_lastFullMessageSentNumber);
      SchemaElement delta = null;
      try
      {
        delta = SchemaElement.createFromDelta(m_lastFullMessageSent, e);
      }
      catch (TypeMismatchException e1)
      {
        // Set ms back to null so as to force creation of a message segment
        // (below)
        ms = null;
      }
      catch (CannotComputeDeltaException e1)
      {
        // Set ms back to null so as to force creation of a message segment
        // (below)
        ms = null;
      }
      BitSequence out = null;
      try
      {
        out = delta.toBitSequence(true);
      }
      catch (BitFormatException e1)
      {
        // Cannot output delta as a bit sequence: happens when some
        // integer element varies by more than its allowed range
        // Set ms back to null so as to force creation of a complete message segment
        ms = null;
      }
      if (ms != null)
      {
        ms.setContents(out);
        m_deltaSegmentsSentSinceLast++;
        m_deltaSegmentsSent++;
        m_deltaSegmentBitsSent += out.size();
      }
    }
    if (ms == null)
    {
      // It is time to create a message segment
      ms = new MessageSegment();
      ms.setSchemaNumber(number);
      try
      {
        ms.setContents(e.toBitSequence());
      } catch (BitFormatException e1)
      {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      m_deltaSegmentsSentSinceLast = 0;
      m_lastFullMessageSent = e;
      m_lastFullMessageSentNumber = m_sequenceNumber;
      m_messageSegmentsSent++;
      m_messageSegmentBitsSent += ms.getSize();
    }
    ms.setSequenceNumber(m_sequenceNumber);
    // Add to buffer
    m_segmentBuffer.add(ms);
    // Add to repeat buffer
    m_segmentToRepeatBuffer.add(ms);
    // Update sequence number
    m_sequenceNumber = (m_sequenceNumber + 1) % Segment.MAX_SEQUENCE;
    if (m_broadcastSchemasEveryN > 0 && m_sequenceNumber % m_broadcastSchemasEveryN == 0)
    {
      // It's time to broadcast a schema
      while (!m_schemas.isEmpty())
      {
        m_lastSchemaSent = (m_lastSchemaSent + 1) % SchemaSegment.SCHEMA_NUMBER_MAX;
        if (!m_schemas.containsKey(m_lastSchemaSent))
        {
          continue;
        }
        addSchemaMessage(m_lastSchemaSent);
        break;
      }
    }
    if (m_repeatAfterN > 0)
    {
      while (!m_segmentToRepeatBuffer.isEmpty())
      {
        Segment seg_to_rep = m_segmentToRepeatBuffer.peekFirst();
        int seq_num = seg_to_rep.getSequenceNumber();
        if (m_sequenceNumber - seq_num > m_repeatAfterN)
        {
          // Time to repeat the segment
          m_segmentBuffer.add(seg_to_rep);
          m_segmentToRepeatBuffer.removeFirst();
        }
        else 
        {
          // Since segments are stored in the buffer in increasing sequential no,
          // no further segment will be in the desired interval
          break;
        }
      }
    }
  }
  
  public void addMessage(int number, String contents) throws ReadException, UnknownSchemaException
  {
    if (!m_schemas.containsKey(number))
    {
      // Schema number does not exist: fail
      throw new UnknownSchemaException();
    }
    SchemaElement se = m_schemas.get(number).copy();
    se.readContentsFromString(contents);
    addMessage(number, se);
  }
  
  /**
   * Adds a schema message to the sender's segment buffer
   * @param number The schema to send
   */
  protected void addSchemaMessage(int number)
  {
    if (!m_schemas.containsKey(number))
    {
      // Schema number undefined: fail
      return;
    }
    // Create segment with schema
    SchemaSegment ss = new SchemaSegment();
    // The sequence number is unused in schema segments in the current version of the protocol
    ss.setSequenceNumber(m_sequenceNumber);
    //m_sequenceNumber = (m_sequenceNumber + 1) % Segment.MAX_SEQUENCE;
    ss.setSchemaNumber(number);
    ss.setSchema(m_schemas.get(number));
    // Add to buffer
    m_segmentBuffer.add(ss);
    m_schemaSegmentsSent++;
    int seg_size = ss.getSize();
    m_schemaSegmentBitsSent += seg_size;
  }
  
  /**
   * Assigns a given schema to a schema number
   * @param number The number to put the schema in the bank
   * @param se The schema to put
   */
  public void setSchema(int number, SchemaElement se)
  {
    if (number < 0 || number >= SchemaSegment.SCHEMA_NUMBER_MAX)
    {
      // Invalid schema number: fail
      return;
    }
    m_schemas.put(number, se);
  }
  
  /**
   * Assigns a given schema to a schema number, parsing it from a string
   * @param number The number to put the schema in the bank
   * @param se The schema to put
   * @throws ReadException If cannot parse a schema from the string
   */
  public void setSchema(int number, String contents) throws ReadException
  {
    SchemaElement se = SchemaElement.parseSchemaFromString(contents);
    setSchema(number, se);
  }
}
