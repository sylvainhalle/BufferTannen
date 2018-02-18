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
 * Representation of an <i>n</i>-bit integer
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
   * The range of the integer (in bits) when expressed as a delta.
   * Normally should be set smaller than normal range.
   */
  protected int m_deltaRange;
  
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
   * Whether the integer represents a signed quantity or not.
   * If yes, the first bit encodes the sign (0 = positive, 1 = negative),
   * and the remaining bits encode the absolute value. This way,
   * an unsigned (positive) integer can also be properly read
   * if interpreted as a signed integer.
   */
  public boolean m_signed = false;
  
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
    m_deltaRange = DEFAULT_RANGE;
    m_value = value;
  }
  
  public IntegerElement(int value, int range, int delta_range, boolean signed)
  {
    super();
    m_range = range;
    m_value = value;
    m_deltaRange = delta_range;
    m_signed = signed;
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
    StringBuilder out = new StringBuilder();
    out.append("Integer");
    if (m_signed)
    {
      out.append("*");
    }
    out.append("(").append(m_range).append(",").append(m_deltaRange).append(")");
    return out.toString();
  }

  @Override
  public BitSequence toBitSequence(boolean as_delta) throws BitFormatException
  {
    BitSequence bs = new BitSequence();
    if (as_delta)
    {
      // Send a single 1 bit, indicating a change
      bs.add(true);
    }
    if (!m_signed)
    {
      BitSequence new_bs = new BitSequence(m_value, m_range);
      bs.addAll(new_bs);
    }
    else
    {
      if (m_value < 0)
        bs.addAll(new BitSequence("1")); // First bit indicates sign
      else
        bs.addAll(new BitSequence("0"));
      // Encode absolute value over remaining bits
      bs.addAll(new BitSequence(Math.abs(m_value), m_range - 1));
    }
    return bs;
  }

  @Override
  public int fromBitSequence(BitSequence bs, boolean as_delta) throws ReadException
  {
    int bits_read = 0;
    int range = m_range;
    if (as_delta)
    {
      range = m_deltaRange;
      m_signed = true; // Delta integers are always signed
    }
    if (range > bs.size())
    {
      throw new ReadException();
    }
    if (m_signed)
    {
      // Number is signed: read first bit to get sign
      BitSequence sign = bs.truncatePrefix(1);
      int multiple = 1;
      if (sign.intValue() == 1)
      {
        multiple = -1;
      }
      BitSequence value = bs.truncatePrefix(range - 1);
      m_value = multiple * value.intValue();
    }
    else
    {
      BitSequence int_v = bs.truncatePrefix(range);
      m_value = int_v.intValue();
    }
    bits_read += range;
    return bits_read;
  }

  @Override
  public SchemaElement copy()
  {
    IntegerElement ie = new IntegerElement(m_value, m_range, m_deltaRange, m_signed);
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
      // Write delta range
      out.addAll(new BitSequence(m_deltaRange, RANGE_WIDTH));
      // Write whether signed
      out.add(m_signed);
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
    // Read delta range
    if (bs.size() < RANGE_WIDTH)
    {
      throw new ReadException("Cannot read integer range");
    }
    data = bs.truncatePrefix(RANGE_WIDTH);
    bits_read += RANGE_WIDTH;
    m_deltaRange = data.intValue();
    // Read whether integer is signed or not
    if (bs.size() < 1)
    {
      throw new ReadException("Cannot read integer sign");
    }
    data = bs.truncatePrefix(1);
    bits_read++;
    m_signed = (data.intValue() == 1);
    return bits_read;
  }
  
  @Override
  protected void readSchemaFromString(MutableString s) throws ReadException
  {
    s.truncateSubstring("Integer".length());
    if (s.startsWith("*"))
    {
      // Indicates a signed integer
      m_signed = true;
      s.truncateSubstring(1);
    }
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
      MutableString[] parts = sub_range.split(",");
      m_range = Integer.parseInt(parts[0].toString());
      if (m_range <= 0 || m_range >= MAX_RANGE)
      {
        throw new ReadException("Invalid range for Integer");
      }
      if (parts.length == 1)
      {
        m_deltaRange = m_range;
      }
      else if (parts.length == 2)
      {
        m_deltaRange = Integer.parseInt(parts[1].toString());
        if (m_deltaRange <= 0 || m_deltaRange >= MAX_RANGE)
        {
          throw new ReadException("Invalid delta range for Integer");
        }
      }
      else
      {
        throw new ReadException("Invalid range expression for Integer");
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

  @Override
  public void readContentsFromDelta(SchemaElement reference, SchemaElement delta)
      throws ReadException
  {
    if (!(reference instanceof IntegerElement))
    {
      throw new ReadException("Type mismatch in reference element: expected an Integer");
    }
    IntegerElement el = (IntegerElement) reference;
    if (delta instanceof NoChangeElement)
    {
      // No change: copy into self value of reference enum
      m_value = el.m_value;
      return;
    }
    // Change: make sure that delta is of proper type
    if (!(delta instanceof IntegerElement))
    {
      throw new ReadException("Type mismatch in delta element: expected an Integer or a no-change");
    }
    IntegerElement del = (IntegerElement) delta;
    // Everything OK: copy value of reference + delta into self
    m_value = el.m_value + del.m_value;
  }
  
  /**
   * Populates the element's content as a difference between the element to represent,
   * and another element to be used as a reference.
   * <strong>Note:</strong> when represented as delta-elements, integers are always
   * signed.
   * @param reference The element to use as a reference
   * @param new_one The new element
   * @return A Schema element representing the difference between reference and new_one
   */
  protected static SchemaElement populateFromDelta(IntegerElement reference, IntegerElement new_one)
  {
    int difference = new_one.m_value - reference.m_value;
    if (difference == 0)
    {
      return new NoChangeElement();
    }
    return new IntegerElement(difference, reference.m_deltaRange, reference.m_deltaRange, true);
  }
}
