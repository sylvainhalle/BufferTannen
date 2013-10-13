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
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.message.SchemaElement;

/**
 * The receiver is responsible for gathering frames from a communication
 * channel, decoding the segments in each frame, buffering and sending back
 * these frames in sequential order.
 * @author sylvain
 *
 */
public class Receiver
{
  /**
   * A circular buffer of segments received
   */
  protected Segment[] m_receivedSegments;
  
  /**
   * A list that will contain the received messages,
   * properly decoded and in sequential order
   */
  protected LinkedList<SchemaElement> m_receivedMessages;
  
  /**
   * Left bound of circular buffer
   */
  protected int m_bufferBegin = 0;
  
  /**
   * Right bound of circular buffer
   */
  protected int m_bufferEnd = 0;
  
  /**
   * The bank of schemas to interpret the messages
   */
  protected Map<Integer,SchemaElement> m_schemas;
  
  public Receiver()
  {
    super();
    m_schemas = new HashMap<Integer,SchemaElement>();
    m_receivedSegments = new Segment[Segment.MAX_SEQUENCE];
    m_receivedMessages = new LinkedList<SchemaElement>();
  }
  
  public void putBitSequence(BitSequence bs)
  {
    Frame f = new Frame();
    try
    {
      f.fromBitSequence(bs);
    }
    catch (ReadException e)
    {
      e.printStackTrace();
    }
    putFrame(f);
  }
  
  public void putFrame(Frame f)
  {
    for (Segment seg : f)
    {
      if (seg instanceof SchemaSegment)
      {
        // Process schemas right away
        SchemaElement se = ((SchemaSegment) seg).getSchema();
        int s_number = ((SchemaSegment) seg).getSchemaNumber();
        m_schemas.put(s_number, se);
      }
      else if (seg instanceof MessageSegment)
      {
        int seq_no = seg.getSequenceNumber();
        m_receivedSegments[seq_no] = seg;
        m_bufferEnd = Math.max(seq_no + 1, m_bufferEnd);        
      }
    }
    int steps = 0;
    int max_steps = m_bufferEnd - m_bufferBegin;
    if (m_bufferEnd < m_bufferBegin)
    {
      max_steps = Segment.MAX_SEQUENCE - (m_bufferBegin - m_bufferEnd);
    }
    for (int i = m_bufferBegin; steps < max_steps; i = (i + 1) % Segment.MAX_SEQUENCE)
    {
      steps++;
      Segment seg = m_receivedSegments[i];
      if (seg == null)
      {
        break;
      }
      // We can process message segments only if we have the schema to
      // decode them
      MessageSegment ms = (MessageSegment) seg;
      int s_number = ms.getSchemaNumber();
      if (!m_schemas.containsKey(s_number))
      {
        // We don't have it: cannot process any further segment
        break;
      }
      SchemaElement se = m_schemas.get(s_number).copy();
      BitSequence bs = ms.getContents();
      try
      {
        se.fromBitSequence(bs);
      }
      catch (ReadException re)
      {
        // We failed to decode the message: perhaps the schema is outdated
        // Wait until next time
        break;
      }
      // We decoded the message successfully
      m_receivedMessages.add(se);
      m_receivedSegments[i] = null;
      m_bufferBegin = (m_bufferBegin + 1) % Segment.MAX_SEQUENCE;        
    }
  }
  
  public SchemaElement pollMessage()
  {
    if (m_receivedMessages.isEmpty())
    {
      return null;
    }
    return m_receivedMessages.removeFirst();
  }
}
