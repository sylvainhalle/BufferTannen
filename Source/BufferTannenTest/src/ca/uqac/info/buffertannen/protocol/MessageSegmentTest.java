package ca.uqac.info.buffertannen.protocol;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;

public class MessageSegmentTest
{

  @Test
  public void testLength()
  {
    MessageSegment ms = new MessageSegment();
    ms.setSchemaNumber(3);
    ms.setSequenceNumber(10);
    ms.setContents(new BitSequence("00100010111011010110"));
    BitSequence sseq = ms.toBitSequence();
    if (sseq.size() != ms.getSize())
    {
      fail("Declared segment size not equal to actual size");
    }
  }
  
  @Test
  public void testReadBytes()
  {
    MessageSegment ms = new MessageSegment();
    ms.setSchemaNumber(3);
    ms.setSequenceNumber(10);
    ms.setContents(new BitSequence("00100010111011010110"));
    BitSequence sseq = ms.toBitSequence();
    // Crop first bits that are read by the frame
    sseq.truncatePrefix(Segment.TYPE_WIDTH);
    int ssize = sseq.size();
    
    MessageSegment ms2 = new MessageSegment();
    int read_bytes = 0;
    try
    {
      read_bytes = ms2.fromBitSequence(sseq);
    } catch (ReadException e)
    {
      fail("Exception while reading segment");
    }
    if (read_bytes != ssize)
    {
      fail("Declared number of bytes read not equal to segment size");
    }
  }

}
