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

import java.util.Vector;

public class EnumElement extends SchemaElement
{
  protected Vector<String> m_constants;
  
  protected String m_value;
  
  protected static final double LOG_2 = Math.log(2);
  
  /**
   * Number of bits used to encode the number of values in the enum
   * (not the values themselves!).
   */
  public static final int ENUM_SIZE_WIDTH = 4;
  
  public EnumElement()
  {
    super();
    m_constants = new Vector<String>();
    m_value = "";
  }
  
  public void addToSchema(String constant)
  {
    m_constants.add(constant);
  }

  @Override
  public BitSequence toBitSequence()
  {
    int num_bits = (int) Math.ceil((Math.log(m_constants.size()) / LOG_2));
    int pos = m_constants.indexOf(m_value);
    if (pos < 0)
    {
      // Not supposed to happen
      assert false;
    }
    BitSequence bs = null;
    try
    {
      bs = new BitSequence(pos, num_bits);
    } catch (BitFormatException e)
    {
      // Do nothing
    }
    return bs;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    int num_bits = (int) Math.ceil((Math.log(m_constants.size()) / LOG_2));
    if (num_bits < bs.size())
    {
      // Invalid input
      throw new ReadException();
    }
    BitSequence sub = bs.truncatePrefix(num_bits);
    int index = sub.intValue();
    if (index < 0 || index >= m_constants.size())
    {
      // Invalid value
      throw new ReadException();
    }
    m_value = m_constants.get(index);
    return num_bits;
  }

  @Override
  public SchemaElement copy()
  {
    EnumElement out = new EnumElement();
    for (int i = 0; i < m_constants.size(); i++)
    {
      out.m_constants.add(m_constants.get(i));
    }
    out.m_value = m_value;
    return out;
  }

  @Override
  public void put(String path, Object value)
  {
    if (!path.isEmpty())
    {
      return;
    }
    if (!(value instanceof String))
    {
      return;
    }
    String copy_from = (String) value;
    m_value = copy_from;
  }

  @Override
  public SchemaElement get(String path)
  {
    if (path.isEmpty())
    {
      return this;
    }
    return null;
  }

  @Override
  protected String toString(String indent)
  {
    return m_value;
  }

  @Override
  protected String schemaToString(String indent)
  {
    return "EnumElement " + m_constants.toString();
  }
  
  public static void main(String[] args)
  {
    EnumElement ee = new EnumElement();
    ee.addToSchema("CONST1"); ee.addToSchema("CONST2"); ee.addToSchema("SOME OTHER");
    EnumElement instance = (EnumElement) ee.copy();
    instance.put("", "SOME OTHER");
    BitSequence bs = instance.toBitSequence();
    System.out.println(bs);
    EnumElement ee2 = (EnumElement) ee.copy();
    try
    {
      ee2.fromBitSequence(bs);
    }
    catch (ReadException e)
    {
      e.printStackTrace();
    }
  }
  
  public BitSequence schemaToBitSequence()
  {
    BitSequence out = null;
    try
    {
      // Write element type
      out = new BitSequence(SCHEMA_ENUM, SCHEMA_WIDTH);
      // Write number of constants
      out.addAll(new BitSequence(m_constants.size(), ENUM_SIZE_WIDTH));
      // Encode each element of the enum, as a null-terminated string
      for (String element : m_constants)
      {
        SmallsciiElement sse = new SmallsciiElement(element);
        out.addAll(sse.toBitSequence());
      }
    } 
    catch (BitFormatException e)
    {
      // Not supposed to happen
      assert false;
    }
    return out;
  }
  
  @Override
  protected int readSchemaFromBitSequence(BitSequence bs) throws ReadException
  {
    int bits_read = 0;
    BitSequence data;
    // Read number of constants
    if (bs.size() < ENUM_SIZE_WIDTH)
    {
      throw new ReadException("Cannot read number of constants in enumeration");
    }
    data = bs.truncatePrefix(ENUM_SIZE_WIDTH);
    bits_read += ENUM_SIZE_WIDTH;
    int num_constants = data.intValue();
    // Read each constant
    for (int i = 0; i < num_constants; i++)
    {
      if (bs.isEmpty())
      {
        throw new ReadException("Cannot read all constants in enumeration");
      }
      SmallsciiElement ss_constant = new SmallsciiElement();
      int read = ss_constant.fromBitSequence(bs);
      bits_read += read;
      m_constants.add(ss_constant.m_contents);
    }
    return bits_read;
  }

}
