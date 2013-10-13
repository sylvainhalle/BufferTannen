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
  protected LinkedList<Segment> m_receivedSegments;
  
  /**
   * A list that will contain the received messages,
   * properly decoded and in sequential order
   */
  protected LinkedList<SchemaElement> m_receivedMessages;
  
  /**
   * Expected sequence number of next segment
   */
  protected int m_expectedSequenceNumber = 0;
  
  /**
   * Number of messages lost since the beginning of the communication
   */
  protected int m_messagesLost = 0;
  
  /**
   * Maximum difference between expected sequence number and
   * highest sequence number received. Suppose for example this
   * value is set to 10 and the receiver awaits segment n.
   * If segment n+k (k &geq; 10) is received, segment n is declared lost and
   * the expected segment number is incremented to n + k-10.
   */
  protected int m_lostInterval = 10;
  
  /**
   * The bank of schemas to interpret the messages
   */
  protected Map<Integer,SchemaElement> m_schemas;
  
  public Receiver()
  {
    super();
    m_schemas = new HashMap<Integer,SchemaElement>();
    m_receivedSegments = new LinkedList<Segment>();
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
  
  protected void putFrame(Frame f)
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
        insertSegment(seg);       
      }
    }
    // Compute difference between highest and lowest sequence number
    Segment max_seg = m_receivedSegments.peekLast();
    int force_send = max_seg.getSequenceNumber() - m_lostInterval;
    while (!m_receivedSegments.isEmpty())
    {
      Segment seg = m_receivedSegments.peekFirst();
      if (seg == null)
      {
        break;
      }
      int seq_no = seg.getSequenceNumber();
      if (seq_no != m_expectedSequenceNumber)
      {
        if (seq_no >= force_send)
        {
          // We are no forced to handle this segment right away
          break;
        }
      }
      // We can process message segments only if we have the schema to
      // decode them
      MessageSegment ms = (MessageSegment) seg;
      int s_number = ms.getSchemaNumber();
      if (!m_schemas.containsKey(s_number))
      {
        // We don't have it: cannot process any further segment
        if (seq_no < force_send)
        {
          // We are forced to handle this segment
          // Since we can't decode it, we discard it and increment
          // the count of lost segments
          m_receivedSegments.removeFirst();
          m_messagesLost += (seq_no - m_expectedSequenceNumber);
          m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
          continue;
        }
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
        if (seq_no < force_send)
        {
          // We are forced to handle this segment
          // Since we can't decode it, we discard it and increment
          // the count of lost segments
          m_receivedSegments.removeFirst();
          m_messagesLost += (seq_no - m_expectedSequenceNumber);
          m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
          continue;
        }
        // Otherwise, we can wait until next time
        break;
      }
      // We decoded the message successfully
      m_receivedMessages.add(se);
      m_expectedSequenceNumber = (m_expectedSequenceNumber + 1) % Segment.MAX_SEQUENCE;
      m_receivedSegments.removeFirst();        
    }
  }
  
  /**
   * Inserts the segment at its proper location in the buffer, based
   * on its sequential number. Since the sequential number goes back to
   * 0 after reaching its maximum value, special care must be taken to
   * 
   * @param seg The segment to insert
   */
  protected void insertSegment(Segment seg)
  {
    int seq_no = seg.getSequenceNumber();
    int i = 0;
    if (m_receivedSegments.isEmpty())
    {
      m_receivedSegments.add(seg);
      return;
    }
    int first_seg_pos = m_receivedSegments.peekFirst().getSequenceNumber();
    int last_seg_pos = m_receivedSegments.peekLast().getSequenceNumber();
    if (seq_no < first_seg_pos && )
    int distance_to_left = (first_seg.getSequenceNumber() - seq_no);
    int distance_to_right = (seq_no - last_seg.getSequenceNumber());
    if (first_seg.getSequenceNumber() > seq_no)
    {
      m_receivedSegments.addFirst(seg);
      return;
    }
    for (Segment cur_seg : m_receivedSegments)
    {
      i++;
      int cur_no = cur_seg.getSequenceNumber();
      if (cur_no < seq_no)
      {
        m_receivedSegments.add(i, seg);
        break;
      }
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
