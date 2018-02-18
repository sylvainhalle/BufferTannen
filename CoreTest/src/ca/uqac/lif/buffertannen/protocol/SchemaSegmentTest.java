/*
    Buffer Tannen, a binary message protocol
    Copyright (C) 2013-2018  Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uqac.lif.buffertannen.protocol;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.lif.buffertannen.message.BitSequence;
import ca.uqac.lif.buffertannen.message.ReadException;
import ca.uqac.lif.buffertannen.message.SchemaElement;
import ca.uqac.lif.buffertannen.protocol.SchemaSegment;
import ca.uqac.lif.buffertannen.protocol.Segment;

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
