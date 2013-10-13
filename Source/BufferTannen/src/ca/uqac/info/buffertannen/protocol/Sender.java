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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.SchemaElement;

public class Sender
{
  protected LinkedList<Segment> m_segmentBuffer;
  
  protected Map<Integer,SchemaElement> m_schemas;
  
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
   */
  protected int m_broadcastSchemasEveryN = 2;
  
  /**
   * A counter to give sequential numbers to segments
   */
  protected int m_sequenceNumber = 0;
  
  public Sender()
  {
    super();
    m_segmentBuffer = new LinkedList<Segment>();
    m_schemas = new HashMap<Integer,SchemaElement>();
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
    while (total_size < Frame.MAX_LENGTH && !m_segmentBuffer.isEmpty())
    {
      Segment seg = m_segmentBuffer.getFirst();
      total_size += seg.getSize();
      if (total_size < Frame.MAX_LENGTH)
      {
        m_segmentBuffer.removeFirst();
        f.add(seg);
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
    // Create frame with message
    MessageSegment ms = new MessageSegment();
    ms.setSequenceNumber(m_sequenceNumber);
    m_sequenceNumber = (m_sequenceNumber + 1) % Segment.MAX_SEQUENCE;
    ms.setSchemaNumber(number);
    ms.setContents(e.toBitSequence());
    // Add to buffer
    m_segmentBuffer.add(ms);
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
  }
  
  /**
   * Adds a schema message to the sender's segment buffer
   * @param number The schema to send
   */
  public void addSchemaMessage(int number)
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
}
