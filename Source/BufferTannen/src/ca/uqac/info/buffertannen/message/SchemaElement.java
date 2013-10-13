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

public abstract class SchemaElement
{
  
  /**
   * Integer values representing each type of element
   */
  protected static final int SCHEMA_OTHER = 0;
  protected static final int SCHEMA_ENUM = 1;
  protected static final int SCHEMA_SMALLSCII = 2;
  protected static final int SCHEMA_LIST = 3;
  protected static final int SCHEMA_MAP = 4;
  protected static final int SCHEMA_STRING = 5;
  protected static final int SCHEMA_INTEGER = 6;
  
  /**
   * Number of bits used to encode schema element type
   */
  protected static final int SCHEMA_WIDTH = 3;
  
  /**
   * Writes the element's content as a sequence of bits
   * @return The sequence of bits corresponding to the element's content
   */
  public abstract BitSequence toBitSequence();
  
  /**
   * Populates an element's contents from a sequence of bits 
   * @param bs The bit sequence to read from
   * @return The number of bits read from the sequence
   * @throws ReadException If the contents of the element cannot be read
   *   from the bit sequence
   */
  public abstract int fromBitSequence(BitSequence bs) throws ReadException;
  
  /**
   * Creates an exact copy (deep clone) of the element
   * @return The copy
   */
  public abstract SchemaElement copy();
  
  public abstract void put(String path, Object value) throws TypeMismatchException;
  
  /**
   * Gets the schema element at the end of the path expression
   * @param path The path expression
   * @return The schema element at the end of the path; null if cannout
   *   be found
   */
  public abstract SchemaElement get(String path);
  
  @Override
  public String toString()
  {
    return toString("");
  }
  
  protected abstract String toString(String indent);
  
  /**
   * Returns the message's <em>schema</em> as a string representation
   * @return The schema
   */
  public String schemaToString()
  {
    return schemaToString("");
  }
  
  protected abstract String schemaToString(String indent);
  
  /**
   * Writes the schema corresponding to the message as a sequence of
   * bits
   * @return The sequence of bits encoding the message schema
   */
  public abstract BitSequence schemaToBitSequence();
  
  public static ElementInt bitSequenceToSchema(BitSequence bs) throws ReadException
  {
    ElementInt ei = new ElementInt();
    int bits_read = 0;
    SchemaElement el = null;
    BitSequence data;
    if (bs.size() < SCHEMA_WIDTH)
    {
      throw new ReadException("Cannot read element type");
    }
    data = bs.truncatePrefix(SCHEMA_WIDTH);
    bits_read += SCHEMA_WIDTH;
    int element_type = data.intValue();
    switch (element_type) {
    case SCHEMA_ENUM:
      el = new EnumElement();
      break;
    case SCHEMA_SMALLSCII:
      el = new SmallsciiElement();
      break;
    case SCHEMA_LIST:
      el = new ListElement();
      break;
    case SCHEMA_MAP:
      el = new FixedMapElement();
      break;
    case SCHEMA_INTEGER:
      el = new IntegerElement();
      break;
    default:
      throw new ReadException("Unknown element type");
    }
    bits_read = el.readSchemaFromBitSequence(bs);
    ei.m_element = el;
    ei.m_int = bits_read;
    return ei;
  }
  
  /**
   * Parses the definition of a schema from a character string 
   * @param s The string to read from
   * @return The schema instance produced from the string
   * @throws ReadException If the string does not conform to the
   *   expected syntax
   */
  public static SchemaElement parseSchemaFromString(String s) throws ReadException
  {
    MutableString ms = new MutableString(s);
    return parseSchemaFromString(ms);
  }
  
  /**
   * Parses the definition of a schema from a character string 
   * @param s The string to read from
   * @return The schema instance produced from the string
   * @throws ReadException If the string does not conform to the
   *   expected syntax
   */
  protected static SchemaElement parseSchemaFromString(MutableString s) throws ReadException
  {
    if (s == null)
    {
      return null;
    }
    s.trim();
    SchemaElement out = null;
    if (s.startsWith("FixedMap"))
    {
      out = new FixedMapElement();
    }
    else if (s.startsWith("List"))
    {
      out = new ListElement();
    }
    else if (s.startsWith("Integer"))
    {
      out = new IntegerElement();
    }
    else if (s.startsWith("Smallscii"))
    {
      out = new SmallsciiElement();
    }
    else
    {
      throw new ReadException("Cannot determine element");
    }
    out.readSchemaFromString(s);
    return out;
  }
  
  protected abstract void readSchemaFromString(MutableString s) throws ReadException;
  
  public void readContentsFromString(String s) throws ReadException
  {
    MutableString ms = new MutableString(s);
    readContentsFromString(ms);
  }
  
  protected abstract void readContentsFromString(MutableString s) throws ReadException;
  
  protected abstract int readSchemaFromBitSequence(BitSequence bs) throws ReadException;
  
  public static class ElementInt
  {
    public SchemaElement m_element;
    public int m_int;
  }
  
  protected static int findMatchingClosing(String s)
  {
    MutableString ms = new MutableString(s);
    return findMatchingClosing(ms);    
  }
  
  protected static int findMatchingClosing(MutableString s)
  {
    int level = 1, pos = 0;
    while (level > 0 && pos < s.length())
    {
      pos++;
      MutableString c = s.substring(pos, pos + 1);
      if (c.is("(") || c.is("{") || c.is("["))
      {
        level++;
      }
      else if (c.is(")") || c.is("}") || c.is("]"))
      {
        level--;
      }
    }
    return pos;
  }
}
