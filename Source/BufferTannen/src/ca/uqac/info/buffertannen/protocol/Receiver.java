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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import ca.uqac.info.buffertannen.message.BitFormatException;
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
   * A buffer for the binary contents received from blob
   * segments
   */
  protected BitSequence m_binaryBuffer;
  
  /**
   * The sequence number of the last segment processed
   * (whether it was discarded, decoded or not) 
   */
  protected int m_lastSegmentNumberSeen = 0;
   
  /**
   * A character string representing the resource name contained in this
   * stream of data. Typically, this is used to provide a filename for
   * the data transmitted.
   */
  protected String m_resourceIdentifier = "";
  
  /**
   * The index of the data stream currently being processed
   */
  protected int m_dataStreamIndex = 0; 
  
  /**
   * The total number of segments contained in this transmission.
   * When set to a nonzero value, indicates that data is transmitted
   * in "lake" mode; when set to 0, data is transmitted in "stream"
   * mode.
   */
  protected int m_totalSegments = 0;

  /**
   * Expected sequence number of next segment
   */
  protected int m_expectedSequenceNumber = -1;

  /**
   * Number of messages lost since the beginning of the communication
   */
  protected int m_messagesLost = 0;
  
  /**
   * The sequence number of the last processed segment
   */
  protected int m_lastProcessedSequenceNumber = -1;

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
   * The maximum length of a frame, in bits
   */
  protected int m_maxFrameLength = 512; 
  
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
   * Number of bits received as blob segments
   */
  protected int m_blobSegmentBitsReceived = 0;
  
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
    m_binaryBuffer = new BitSequence();
  }
  
  public int getLastSegmentNumberSeen()
  {
    return m_lastSegmentNumberSeen;
  }
  
  public Sender.SendingMode getSendingMode()
  {
    if (m_totalSegments > 0)
    {
      return Sender.SendingMode.LAKE;
    }
    return Sender.SendingMode.STREAM;
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
    return m_deltaSegmentBitsReceived + m_schemaSegmentBitsReceived + m_messageSegmentBitsReceived + m_blobSegmentBitsReceived;
  }
  
  /**
   * Attempts to read a number of bytes from the binary buffer.
   * @param length The number of bytes to read. -1 to get the whole
   *   buffer
   * @return A bit sequence of the prescribed length. If the buffer
   *   contains less data than the desired length, the whole contents
   *   of the buffer will be returned. It is hence up to the receiver
   *   to check the length of the returned sequence.
   */
  public BitSequence pollBinaryBuffer(int length)
  {
    if (length < 0)
    {
      length = m_binaryBuffer.size();
    }
    return m_binaryBuffer.truncatePrefix(length);
  }
  
  public void putBitSequence(String base64)
  {
    BitSequence bs = new BitSequence();
    try
    {
      bs.fromBase64(base64);
      putBitSequence(bs);
    }
    catch (BitFormatException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
  
  public boolean[] getBufferStatus()
  {
    if (m_totalSegments < 0)
    {
      // Does not apply for stream mode
      return null;
    }
    boolean[] out = new boolean[m_totalSegments];
    int i = 0;
    for (; i <= m_lastProcessedSequenceNumber; i++)
    {
      out[i] = true;
    }
    for (Segment seg : m_receivedSegments)
    {
      int seq_no = seg.getSequenceNumber();
      if (i != seq_no)
      {
        System.err.println("ERROR: unexpected sequence number");
      }
      if (seg instanceof PlaceholderSegment)
      {
        out[i] = false;
      }
      else
      {
        out[i] = true;
      }
      i++;
    }
    for (int j = i; j < m_totalSegments; j++)
    {
      out[j] = false;
    }
    return out;
  }
  
  public int getDataStreamIndex()
  {
    return m_dataStreamIndex;
  }
  
  public String getResourceIdentifier()
  {
    return m_resourceIdentifier;
  }
  
  protected void putFrame(Frame f)
  {
    // Analyze contents of frame
    m_totalSegments = f.getTotalSegments();
    if (m_totalSegments > 0)
    {
      // This indicates we are in "lake" mode: segments will
      // be sent over and over. Therefore, we should never declare
      // a segment as lost. We do this by setting the lostInterval
      // to a value greater than the total number of expected segments
      m_lostInterval = m_totalSegments + 1;
    }
    m_dataStreamIndex = f.getDataStreamIndex();
    m_resourceIdentifier = f.getResourceIdentifier();
    for (Segment seg : f)
    {
      if (seg instanceof SchemaSegment)
      {
        // Update schema bank with received schema segment
        SchemaSegment ss = (SchemaSegment) seg;
        SchemaElement se = ss.getSchema();
        int s_number = ss.getSchemaNumber();
        m_schemas.put(s_number, se);
        // Update stats
        m_schemaSegmentsReceived++;
        m_schemaSegmentBitsReceived += ss.getSize();
        printMessage("Received schema " + s_number, 2);   
      }
      else
      {
        // Insert/replace segment in buffer at proper location
        insertInBuffer(seg);
      }
    }
    // Check if some received segments can be processed
    if (m_receivedSegments.isEmpty())
    {
      // No
      return;
    }
    // All segments with number lower than force_send_index still in the buffer
    // must be sent
    Segment last_segment = m_receivedSegments.peekLast();
    int force_send_index = last_segment.getSequenceNumber() - m_lostInterval;
    Iterator<Segment> seg_it = m_receivedSegments.iterator();
    while (seg_it.hasNext())
    {
      Segment seg = seg_it.next();
      if (seg instanceof BlobSegment)
      {
        // Blob segments are handled separately; their binary contents
        // are sent in a binary buffer
        BlobSegment blob = (BlobSegment) seg;
        int seg_seq_no = blob.getSequenceNumber();
        BitSequence bs = blob.getContents();
        m_binaryBuffer.addAll(bs);
        m_lastProcessedSequenceNumber = seg_seq_no;
        m_blobSegmentBitsReceived += bs.size();
        printMessage("Processed blob segment " + seg_seq_no, 2);
        seg_it.remove();
      }
      else if (seg instanceof DeltaSegment)
      {
        DeltaSegment ds = (DeltaSegment) seg;
        int seg_seq_no = ds.getSequenceNumber();
        int ref_segment_no = ds.getDeltaToWhat();
        // We can process delta segments only if we have the reference segment AND the schema
        boolean contains_ref_message = m_referenceMessages.containsKey(ref_segment_no);
        boolean contains_ref_schema = m_referenceSchemas.containsKey(ref_segment_no);
        if (!contains_ref_message || !contains_ref_schema)
        {
          if (!contains_ref_message)
          {
            printMessage("Cannot process delta segment " + ds.getSequenceNumber() + ": missing reference segment " + ref_segment_no, 2);
          }
          if (!contains_ref_schema)
          {
            printMessage("Cannot process delta segment " + ds.getSequenceNumber() + ": missing reference schema", 2);
          }
          // We can't, because we are missing something
          if (seg_seq_no < force_send_index)
          {
            // We must process it right now; declare segment as lost
            printMessage("Delta segment " + seg_seq_no + " declared lost", 2);
            seg_it.remove();
            m_lastProcessedSequenceNumber = seg_seq_no;
            m_messagesLost++;
            continue;
          }
          else
          {
            // No need to process it right now: wait until next time
            break;
          }
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
          printMessage("Failed to decode delta segment " + seg_seq_no, 1);
          re.printStackTrace();
          // We failed to decode the message: perhaps the schema is outdated
          if (seg_seq_no < force_send_index)
          {
            // We are forced to handle this segment
            printMessage("Delta segment " + seg_seq_no + " declared lost", 2);
            seg_it.remove();
            m_lastProcessedSequenceNumber = seg_seq_no;
            m_messagesLost++;
            continue;
          }
          // Otherwise, we can wait until next time
          break;
        }
        // We decoded the segment successfully
        m_referenceMessages.put(seg_seq_no, se);
        m_referenceSchemas.put(seg_seq_no, reference_schema);
        printMessage("Successfully processed delta segment " + seg_seq_no, 2);
        m_deltaSegmentBitsReceived += bits_received;
        m_deltaSegmentsReceived++;
        m_receivedMessages.add(se);
        m_lastProcessedSequenceNumber = seg_seq_no;
        seg_it.remove();
      }
      else if (seg instanceof MessageSegment)
      {
        MessageSegment ms = (MessageSegment) seg;
        int seg_seq_no = ms.getSequenceNumber();
        int schema_number = ms.getSchemaNumber();
        if (m_schemas.containsKey(schema_number))
        {
          SchemaElement ref_schema = m_schemas.get(schema_number).copy();
          SchemaElement se = m_schemas.get(schema_number).copy();
          BitSequence bs = ms.getContents();
          int bits_received = bs.size();
          try
          {
            se.fromBitSequence(bs);
          }
          catch (ReadException re)
          {
            // We failed to decode the message
            if (seg_seq_no < force_send_index)
            {
              // We are forced to handle this segment
              printMessage("Message segment " + seg_seq_no + " declared lost", 2);
              m_lastProcessedSequenceNumber = seg_seq_no;
              seg_it.remove();
              m_messagesLost++;
              continue;
            }
            // Otherwise, we can wait until next time
            break;
          }
          // We decoded the segment successfully
          m_referenceMessages.put(seg_seq_no, se);
          m_referenceSchemas.put(seg_seq_no, ref_schema);
          printMessage("Successfully processed message segment " + seg_seq_no, 2);
          m_messageSegmentBitsReceived += bits_received;
          m_messageSegmentsReceived++;
          m_receivedMessages.add(se);
          m_lastProcessedSequenceNumber = seg_seq_no;
          seg_it.remove();
        }
        else
        {
          // We failed to decode the message
          if (seg_seq_no < force_send_index)
          {
            // We are forced to handle this segment
            printMessage("Message segment " + seg_seq_no + " declared lost", 2);
            m_lastProcessedSequenceNumber = seg_seq_no;
            seg_it.remove();
            m_messagesLost++;
            continue;
          }
          // Otherwise, we can wait until next time
          break;
        }
      }
      else if (seg instanceof PlaceholderSegment)
      {
        int seg_seq_no = seg.getSequenceNumber();
        if (seg_seq_no < force_send_index)
        {
          // No segment there, but we are forced to process it
          printMessage("Segment " + seg_seq_no + " (of unknown type) declared lost", 2);
          m_messagesLost++;
          m_lastProcessedSequenceNumber = seg_seq_no;
          seg_it.remove();
        }
        else
        {
          // No need to force sending: stop
          break;
        }
      }
    }
  }
  
  /**
   * Insert a segment at the proper location in the segment buffer, based
   * on its sequential number.
   * @param seg The segment to insert
   */
  protected void insertInBuffer(Segment seg)
  {
    int seg_seq_no = seg.getSequenceNumber();
    m_lastSegmentNumberSeen = seg_seq_no;
    if (seg_seq_no <= m_lastProcessedSequenceNumber)
    {
      // We have already seen and processed that segment
      printMessage("Segment " + seg_seq_no + " already processed", 2);
      return;
    }
    if (m_receivedSegments.isEmpty())
    {
      for (int j = m_lastProcessedSequenceNumber + 1; j < seg_seq_no; j++)
      {
        // Insert placeholder segments if there is a gap between the
        // segment to add
        m_receivedSegments.add(new PlaceholderSegment(j));
      } 
      // Insert here
      m_receivedSegments.add(seg);
      return;
    }
    int i = 0;
    int list_seg_no = 0;
    for (Segment list_seg : m_receivedSegments)
    {
      list_seg_no = list_seg.getSequenceNumber();
      if (list_seg_no == seg_seq_no)
      {
        // Replace segment currently at that location by the one just received
        if (list_seg instanceof PlaceholderSegment)
        {
          m_receivedSegments.remove(i);
          m_receivedSegments.add(i, seg);
        }
        else
        {
          printMessage("Segment " + seg_seq_no + " already in buffer", 2);
        }
        break;
      }
      i++;
    }
    if (seg_seq_no > list_seg_no)
    {
      for (int j = list_seg_no + 1; j < seg_seq_no; j++)
      {
        // Insert placeholder segments if there is a gap between the
        // segment to add and the last one received
        m_receivedSegments.add(new PlaceholderSegment(j));
      }
      // Insert here
      m_receivedSegments.add(seg);      
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
