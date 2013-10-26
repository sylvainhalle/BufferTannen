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

import java.io.PrintStream;
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
   * A buffer of reference messages received (used to process delta-segments)
   */
  protected Map<Integer,SchemaElement> m_referenceMessages;
  
  /**
   * A buffer of reference schema elements received (used to process delta-segments)
   */
  protected Map<Integer,SchemaElement> m_referenceSchemas;

  /**
   * A list that will contain the received messages,
   * properly decoded and in sequential order
   */
  protected LinkedList<SchemaElement> m_receivedMessages;

  /**
   * Expected sequence number of next segment
   */
  protected int m_expectedSequenceNumber = -1;

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
  protected int m_lostInterval = 21;

  /**
   * The bank of schemas to interpret the messages
   */
  protected Map<Integer,SchemaElement> m_schemas;
  
  /**
   * Verbosity for standard out; a value of 0 won't print anything
   */
  protected int m_verbosity = 0;
  
  /* --- Various statistics about segments received --- */
  
  /**
   * Number of raw bits received (including retransmissions)
   */
  protected int m_rawBitsReceived = 0;
  
  /**
   * Number of bits received as schema segments
   */
  protected int m_schemaSegmentBitsReceived = 0;
  
  /**
   * Number of bits received as message segments
   */
  protected int m_messageSegmentBitsReceived = 0;
  
  /**
   * Number of bits received as delta segments
   */
  protected int m_deltaSegmentBitsReceived = 0;
  
  /**
   * Number of bits received in <em>distinct</em> schema segments
   */
  protected int m_schemaSegmentDistinctBitsReceived = 0;
  
  /**
   * Number of bits received in <em>distinct</em> message segments
   */
  protected int m_messageSegmentDistinctBitsReceived = 0;
  
  /**
   * Number of bits received in <em>distinct</em> delta segments
   */
  protected int m_deltaSegmentDistinctBitsReceived = 0;
  
  /**
   * Number of schema segments received
   */
  protected int m_schemaSegmentsReceived = 0;
  
  /**
   * Number of message segments received
   */
  protected int m_messageSegmentsReceived = 0;
  
  /**
   * Number of delta segments received
   */
  protected int m_deltaSegmentsReceived = 0;
  
  /**
   * The print stream to output messages from the receiver
   * (typically System.out or System.err, or null to
   * disable printing)
   */
  protected PrintStream m_console = null;

  public Receiver()
  {
    super();
    m_schemas = new HashMap<Integer,SchemaElement>();
    m_receivedSegments = new LinkedList<Segment>();
    m_referenceMessages = new HashMap<Integer,SchemaElement>();
    m_referenceSchemas = new HashMap<Integer,SchemaElement>();
    m_receivedMessages = new LinkedList<SchemaElement>();
  }

  public int getMessageLostCount()
  {
    return m_messagesLost;
  }
  
  public int getNumberOfMessageSegments()
  {
    return m_messageSegmentsReceived;
  }
  
  public int getNumberOfSchemaSegments()
  {
    return m_schemaSegmentsReceived;
  }
  
  public int getNumberOfDeltaSegments()
  {
    return m_deltaSegmentsReceived;
  }
  
  public int getNumberOfMessageSegmentsBits()
  {
    return m_messageSegmentBitsReceived;
  }
  
  public int getNumberOfSchemaSegmentsBits()
  {
    return m_schemaSegmentBitsReceived;
  }
  
  public int getNumberOfDeltaSegmentsBits()
  {
    return m_deltaSegmentBitsReceived;
  }
  
  public int getNumberOfRawBits()
  {
    return m_rawBitsReceived;
  }
  
  public int getNumberOfDistinctBits()
  {
    return m_deltaSegmentBitsReceived + m_schemaSegmentBitsReceived + m_messageSegmentBitsReceived;
  }

  public void putBitSequence(BitSequence bs)
  {
    Frame f = new Frame();
    m_rawBitsReceived += bs.size();
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
  
  /**
   * Sets the print stream to send messages to
   * @param out A print stream (typically <tt>System.err</tt> or
   * <tt>System.out</tt>, null to disable messages
   */
  public void setConsole(PrintStream out)
  {
    m_console = out;
  }
  
  protected void printMessage(String message, int verbosity_level)
  {
    if (m_console != null && m_verbosity >= verbosity_level)
    {
      m_console.println(message);
    }
  }
  
  /**
   * Sets the verbosity level of the receiver
   * @param level The verbosity level
   */
  public void setVerbosity(int level)
  {
    m_verbosity = level;
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
        printMessage("Received schema " + s_number, 2);
        m_schemaSegmentsReceived++;
        m_schemaSegmentBitsReceived += seg.getSize();
      }
      else if (seg instanceof DeltaSegment) // Must appear first, as DeltaSegment is a child of MessageSegment
      {
        DeltaSegment ds = (DeltaSegment) seg;
        printMessage("Received delta segment " + seg.getSequenceNumber() + " referring to segment " + ds.getDeltaToWhat(), 2);
        insertSegment(seg);
      }
      else if (seg instanceof MessageSegment)
      {
        printMessage("Received message segment " + seg.getSequenceNumber(), 2);
        insertSegment(seg);
      }
    }
    if (m_receivedSegments.isEmpty())
    {
      // Nothing more to do: segment buffer is empty
      return;
    }
    // We still haven't processed any frame; as we may pick an already
    // ongoing transmission, set sequence number to smallest one
    // received and start expecting segments from that number on
    if (m_expectedSequenceNumber == -1)
    {
      Segment min_seg = m_receivedSegments.peekFirst();
      m_expectedSequenceNumber = min_seg.getSequenceNumber();
      printMessage("Setting sequence number to " + m_expectedSequenceNumber, 2);
    }
    // Compute difference between highest and lowest sequence number
    int force_send = 0;
    Segment max_seg = m_receivedSegments.peekLast();
    if (max_seg != null)
    {
      force_send = max_seg.getSequenceNumber() - m_lostInterval;

    }
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
      if (seg instanceof DeltaSegment)
      {
        DeltaSegment ds = (DeltaSegment) seg;
        int ref_segment_no = ds.getDeltaToWhat();
        // We can process delta segments only if we have the reference segment AND the schema
        if (!m_referenceMessages.containsKey(ref_segment_no) || !m_referenceSchemas.containsKey(ref_segment_no))
        {
          if (!m_referenceMessages.containsKey(ref_segment_no))
          {
            printMessage("Cannot process delta segment " + ds.getSequenceNumber() + ": missing reference segment " + ref_segment_no, 2);
          }
          else if (!m_schemas.containsKey(m_referenceSchemas.get(ref_segment_no)))
          {
            printMessage("Cannot process delta segment " + ds.getSequenceNumber() + ": missing reference schema", 2);
          }
          // We are missing one of them: cannot process any further segment
          if (seq_no < force_send)
          {
            // We are forced to handle this segment
            // Since we can't decode it, we discard it and increment
            // the count of lost segments
            printMessage("Delta segment " + seq_no + " declared lost", 2);
            m_receivedSegments.removeFirst();
            m_messagesLost += Math.abs(seq_no - m_expectedSequenceNumber + 1);
            m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
            continue;
          }
          break;
        }
        SchemaElement reference_schema = m_referenceSchemas.get(ref_segment_no);
        SchemaElement reference_element = m_referenceMessages.get(ref_segment_no).copy();
        SchemaElement delta_element = reference_schema.copy();
        SchemaElement se = reference_schema.copy();
        BitSequence bs = ds.getContents();
        int bits_received = bs.size();
        try
        {
          SchemaElement.ElementInt ei = delta_element.readContentsFromBitSequence(bs, true);
          delta_element = ei.m_element;
          se.readContentsFromDelta(reference_element, delta_element);
        }
        catch (ReadException re)
        {
          printMessage("Failed to decode delta segment " + seq_no, 1);
          re.printStackTrace();
          
          // We failed to decode the message: perhaps the schema is outdated
          if (seq_no < force_send)
          {
            // We are forced to handle this segment
            // Since we can't decode it, we discard it and increment
            // the count of lost segments
            printMessage("Message segment " + seq_no + " declared lost", 2);
            m_receivedSegments.removeFirst();
            m_messagesLost += Math.abs(seq_no - m_expectedSequenceNumber + 1);
            m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
            continue;
          }
          // Otherwise, we can wait until next time
          break;
        }
        // We decoded the message successfully
        if (seq_no < force_send)
        {
          // ...but we were forced to
          m_messagesLost += (seq_no - m_expectedSequenceNumber);
          printMessage("**Lost " + (seq_no - m_expectedSequenceNumber) + " messages", 2);
        }
        printMessage("Successfully processed delta segment " + seq_no, 2);
        m_deltaSegmentBitsReceived += bits_received;
        m_deltaSegmentsReceived++;
        m_receivedMessages.add(se);
        m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
        m_receivedSegments.removeFirst();
      }
      else if (seg instanceof MessageSegment)
      {
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
            m_messagesLost += Math.abs(seq_no - m_expectedSequenceNumber);
            m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
            continue;
          }
          break;
        }
        SchemaElement ref_schema = m_schemas.get(s_number).copy();
        SchemaElement se = m_schemas.get(s_number).copy();
        BitSequence bs = ms.getContents();
        int bits_received = bs.size();
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
            discardSegment(seq_no, force_send);
            continue;
          }
          // Otherwise, we can wait until next time
          break;
        }

        // We decoded the message successfully
        if (seq_no < force_send)
        {
          // ...but we were forced to
          m_messagesLost += (seq_no - m_expectedSequenceNumber);
          printMessage("**Lost " + (seq_no - m_expectedSequenceNumber) + " messages", 2);
        }
        m_referenceMessages.put(seq_no, se);
        m_referenceSchemas.put(seq_no, ref_schema);
        printMessage("Successfully processed message segment " + seq_no, 2);
        m_messageSegmentBitsReceived += bits_received;
        m_messageSegmentsReceived++;
        m_receivedMessages.add(se);
        m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
        m_receivedSegments.removeFirst();
        
      }
    }
  }
  
  protected void discardSegment(int seq_no, int force_send)
  {
    if (seq_no < force_send)
    {
      // We are forced to handle this segment
      // Since we can't decode it, we discard it and increment
      // the count of lost segments
      m_receivedSegments.removeFirst();
      m_messagesLost += (seq_no - m_expectedSequenceNumber);
      m_expectedSequenceNumber = (seq_no + 1) % Segment.MAX_SEQUENCE;
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
    if (seq_no < m_expectedSequenceNumber)
    {
      // This segment is a repetition of one we already processed: ignore
      printMessage("Segment already seen: " + seq_no, 2);
      return;
    }
    if (m_receivedSegments.isEmpty())
    {
      addSegment(seg);
      return;
    }
    int first_seg_pos = m_receivedSegments.peekFirst().getSequenceNumber();
    //int last_seg_pos = m_receivedSegments.peekLast().getSequenceNumber();
    if (first_seg_pos > seq_no)
    {
      m_receivedSegments.addFirst(seg);
      return;
    }
    boolean added = false;
    for (Segment cur_seg : m_receivedSegments)
    {
      i++;
      int cur_no = cur_seg.getSequenceNumber();
      if (cur_no > seq_no)
      {
        addSegment(i-1, seg);
        added = true;
        break;
      }
      else if (cur_no == seq_no)
      {
        // This segment is already in the buffer: ignore
        printMessage("Segment already in buffer: " + seq_no, 2);
        added = true;
        break;
      }
    }
    if (!added)
    {
      addSegmentLast(seg); // Add last
    }
  }

  /**
   * Add a segment at a given position in the buffer of received segments.
   * If the segment is a message segment, add it also to the map of reference
   * segments.
   * @param position The position to add the segment to
   * @param seg The segment to add
   */
  protected void addSegment(int position, Segment seg)
  {
    m_receivedSegments.add(position, seg);
    /*if (seg instanceof MessageSegment)
    {
      // Add segment to reference segments
      MessageSegment ms = (MessageSegment) seg;
      m_referenceSegments.put(ms.getSequenceNumber(), ms);
    }*/
  }

  /**
   * Add a segment at in the buffer of received segments.
   * If the segment is a message segment, add it also to the map of reference
   * segments.
   * @param seg The segment to add
   */
  protected void addSegment(Segment seg)
  {
    m_receivedSegments.add(seg);
    /*if (seg instanceof MessageSegment)
    {
      // Add segment to reference segments
      MessageSegment ms = (MessageSegment) seg;
      m_referenceMessages.put(ms.getSequenceNumber(), ((MessageSegment) seg).getContents());
    }*/
  }

  /**
   * Add a segment at the end of the buffer of received segments.
   * If the segment is a message segment, add it also to the map of reference
   * segments.
   * @param seg The segment to add
   */
  protected void addSegmentLast(Segment seg)
  {
    m_receivedSegments.addLast(seg);
    /*if (seg instanceof MessageSegment)
    {
      // Add segment to reference segments
      MessageSegment ms = (MessageSegment) seg;
      m_referenceSegments.put(ms.getSequenceNumber(), ms);
    }*/
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
