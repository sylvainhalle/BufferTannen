package ca.uqac.info.buffertannen.protocol;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.message.SchemaElement;

public class SchemaSegmentTest
{

  @Test
  public void testLength()
  {
    SchemaElement se = null;
    try
    {
      se = SchemaElement.parseSchemaFromString("FixedMap { \"a\" : Smallscii, \"b\" : Integer(3) }");
    } catch (ReadException e)
    {
      fail("Cannot parse schema");
    }
    SchemaSegment ms = new SchemaSegment();
    ms.setSchemaNumber(3);
    ms.setSequenceNumber(10);
    ms.setSchema(se);
    BitSequence sseq = ms.toBitSequence();
    if (sseq.size() != ms.getSize())
    {
      fail("Declared segment size (" + ms.getSize() + ") not equal to actual size (" + sseq.size() + ")");
    }
  }
  
  @Test
  public void testReadBytes()
  {
    SchemaElement se = null;
    try
    {
      se = SchemaElement.parseSchemaFromString("FixedMap { \"a\" : Smallscii, \"b\" : Integer(3) }");
    } catch (ReadException e)
    {
      fail("Cannot parse schema");
    }
    SchemaSegment ms = new SchemaSegment();
    ms.setSchemaNumber(3);
    ms.setSequenceNumber(10);
    ms.setSchema(se);
    BitSequence sseq = ms.toBitSequence();
    // Crop first bits that are read by the frame
    sseq.truncatePrefix(Segment.TYPE_WIDTH);
    int ssize = sseq.size();
    
    SchemaSegment ms2 = new SchemaSegment();
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
      fail("Declared number of bytes read (" + read_bytes + ") not equal to segment size (" + ssize + ")");
    }
  }

}
