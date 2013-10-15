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
package ca.uqac.info.buffertannen.message;

import ca.uqac.info.util.MutableString;

/**
 * Representation of an <i>n</i>-bit, positive integer
 * @author sylvain
 *
 */
public class IntegerElement extends SchemaElement
{
  protected int m_value;
  
  /**
   * The range of the integer (in bits)
   */
  protected int m_range;
  
  /**
   * Default range if left unspecified
   */
  protected static final int DEFAULT_RANGE = 16;
  
  /**
   * Number of bits to encode range. Look out! This is not
   * the range itself, which may be anything between 1 and 2^<sup>RANGE_WIDTH</sup>
   */
  public static final int RANGE_WIDTH = 5;
  
  /**
   * Maximum integer range that can be encoded with the bits
   * (i.e. 2^<sup>RANGE_WIDTH</sup>)
   */
  public static final int MAX_RANGE = (int) Math.pow(2,  RANGE_WIDTH);
  
  public IntegerElement()
  {
    super();
    m_range = DEFAULT_RANGE;
    m_value = 0;
  }
  
  public IntegerElement(int value)
  {
    super();
    m_range = DEFAULT_RANGE;
    m_value = value;
  }
  
  public IntegerElement(int value, int range)
  {
    super();
    m_range = range;
    m_value = value;
  }
  
  public SchemaElement get(String path)
  {
    if (path.isEmpty())
    {
      return this;
    }
    return null;
  }
  
  protected String toString(String indent)
  {
    return Integer.toString(m_value);
  }
  
  protected String schemaToString(String indent)
  {
    return "Integer(" + m_range + ")";
  }

  @Override
  public BitSequence toBitSequence()
  {
    BitSequence bs = null;
    try
    {
      bs = new BitSequence(m_value, m_range);
    }
    catch (BitFormatException e)
    {
      e.printStackTrace();
    }
    return bs;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    if (m_range > bs.size())
    {
      throw new ReadException();
    }
    BitSequence int_v = bs.truncatePrefix(m_range);
    m_value = int_v.intValue();
    return m_range;
  }

  @Override
  public SchemaElement copy()
  {
    IntegerElement ie = new IntegerElement(m_value, m_range);
    return ie;
  }

  @Override
  public void put(String path, Object value) throws TypeMismatchException
  {
    if (!path.isEmpty() || !(value instanceof Integer))
    {
      throw new TypeMismatchException();
    }
    Integer copy_from = (Integer) value;
    if (copy_from < 0 || copy_from >= Math.pow(2, m_range))
    {
      throw new TypeMismatchException("Integer value out of range");
    }
    m_value = copy_from;
    
  }

  @Override
  public BitSequence schemaToBitSequence()
  {
    BitSequence out = null;
    try
    {
      // Write element type
      out = new BitSequence(SCHEMA_INTEGER, SCHEMA_WIDTH);
      // Write range
      out.addAll(new BitSequence(m_range, RANGE_WIDTH));
    } 
    catch (BitFormatException e)
    {
      // Not supposed to happen
      assert false;
    }
    return out;
  }
  
  protected int readSchemaFromBitSequence(BitSequence bs) throws ReadException
  {
    int bits_read = 0;
    BitSequence data;
    // Read range
    if (bs.size() < RANGE_WIDTH)
    {
      throw new ReadException("Cannot read integer range");
    }
    data = bs.truncatePrefix(RANGE_WIDTH);
    bits_read += RANGE_WIDTH;
    m_range = data.intValue();
    return bits_read;
  }
  
  @Override
  protected void readSchemaFromString(MutableString s) throws ReadException
  {
    s.truncateSubstring("Integer".length());
    if (s.startsWith("("))
    {
      // Read range if any
      int index = s.indexOf(")");
      if (index < 0)
      {
        throw new ReadException("Invalid definition of an Integer");
      }
      MutableString sub_range = s.substring(1, index);
      s.truncateSubstring(index + 1);
      m_range = Integer.parseInt(sub_range.toString());
      if (m_range <= 0 || m_range >= MAX_RANGE)
      {
        throw new ReadException("Invalid range for Integer");
      }
    }
    return;
  }
  
  @Override
  protected void readContentsFromString(MutableString s) throws ReadException
  {
    int index = s.length(), pos = 0;
    pos = s.indexOf(",");
    if (pos >= 0)
    {
      index = (int) Math.min(index, pos);
    }
    pos = s.indexOf("]");
    if (pos >= 0)
    {
      index = (int) Math.min(index, pos);
    }
    pos = s.indexOf("}");
    if (pos >= 0)
    {
      index = (int) Math.min(index, pos);
    }
    if (index < 0)
    {
      throw new ReadException("Error reading integer value");
    }
    MutableString value = null;
    if (index < s.length())
    {
      value = s.truncateSubstring(index);
    }
    else
    {
      value = new MutableString(s);
      s.clear();
    }
    this.m_value = Integer.parseInt(value.toString());
  }
}
