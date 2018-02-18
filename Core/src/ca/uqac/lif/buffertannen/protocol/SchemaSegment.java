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

import ca.uqac.lif.buffertannen.message.BitFormatException;
import ca.uqac.lif.buffertannen.message.BitSequence;
import ca.uqac.lif.buffertannen.message.ReadException;
import ca.uqac.lif.buffertannen.message.SchemaElement;

public class SchemaSegment extends Segment
{
  
  protected SchemaElement m_schema;
  
  protected int m_schemaNumber = -1;
  
  /**
   * The number of bits used to encode the schema number
   */
  public static final int SCHEMA_NUMBER_WIDTH = 4;
  public static final int SCHEMA_NUMBER_MAX = (int) Math.pow(2, SCHEMA_NUMBER_WIDTH);
  
  public SchemaSegment()
  {
    super();
  }
  
  @Override
  public int getSize()
  {
    return SCHEMA_NUMBER_WIDTH + TYPE_WIDTH + SEQUENCE_WIDTH + m_schema.schemaToBitSequence().size();
  }
  
  /**
   * Sets the schema number that this segment advertises
   * @param number The number
   */
  public void setSchemaNumber(int number)
  {
    if (number < 0 || number >= SCHEMA_NUMBER_MAX)
    {
      // Schema number outside range: fail
      return;
    }
    m_schemaNumber = number;
  }
  
  /**
   * Returns the schema number contained in this segment
   * @return The number
   */
  public int getSchemaNumber()
  {
    return m_schemaNumber;
  }
  
  /**
   * Sets the schema advertised by this segment
   * @param e The schema
   */
  public void setSchema(SchemaElement e)
  {
    m_schema = e;
  }
  
  /**
   * Returns the schema contained in this segment
   * @return The schema
   */
  public SchemaElement getSchema()
  {
    return m_schema;
  }

  @Override
  public BitSequence toBitSequence()
  {
    BitSequence out = new BitSequence();
    if (m_schemaNumber < 0 || m_schemaNumber >= SCHEMA_NUMBER_MAX)
    {
      // Schema number out of range: fail
      return null;
    }
    try
    {
      // Write segment type number
      out.addAll(new BitSequence(SEGMENT_SCHEMA, TYPE_WIDTH));
      // Write sequence number
      out.addAll(new BitSequence(m_sequenceNumber, SEQUENCE_WIDTH));
      // Write schema number
      out.addAll(new BitSequence(m_schemaNumber, SCHEMA_NUMBER_WIDTH));
    }
    catch (BitFormatException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    out.addAll(m_schema.schemaToBitSequence());
    return out;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    int bits_read = 0;
    BitSequence data;
    // Segment type number was already consumed by the frame reading method,
    // so we don't need to process it here
    // Read sequence number
    if (bs.size() < SEQUENCE_WIDTH)
    {
      throw new ReadException("Cannot read segment sequence number");
    }
    data = bs.truncatePrefix(SEQUENCE_WIDTH);
    bits_read += SEQUENCE_WIDTH;
    m_sequenceNumber = data.intValue();
    // Read schema number
    if (bs.size() < SCHEMA_NUMBER_WIDTH)
    {
      throw new ReadException("Cannot read schema number");
    }
    data = bs.truncatePrefix(SCHEMA_NUMBER_WIDTH);
    bits_read += SCHEMA_NUMBER_WIDTH;
    m_schemaNumber = data.intValue();
    // Read schema
    SchemaElement.ElementInt ei = SchemaElement.bitSequenceToSchema(bs);
    bits_read += ei.m_int;
    m_schema = ei.m_element;
    return bits_read;
  }
  
  @Override
  public String toString()
  {
    StringBuilder out = new StringBuilder();
    out.append("Segment type: schema definition\n");
    out.append("Schema number: ").append(m_schemaNumber).append("\n");
    out.append(m_schema.schemaToString()).append("\n");
    return out.toString();
  }

}
