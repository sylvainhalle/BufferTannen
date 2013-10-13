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

import java.util.*;

import ca.uqac.info.util.MutableString;

//import ca.uqac.info.buffertannen.BitSequence.FormatException;

/**
 * Variable-length list of elements (maximum 255), all of which must
 * be of the same type.
 * @author sylvain
 *
 */
public class ListElement extends SchemaElement
{
  protected SchemaElement m_elementType;
  
  protected List<SchemaElement> m_contents;
  
  protected static final int MAX_LENGTH_BITS = 8;
  
  public ListElement()
  {
    super();
    m_contents = new LinkedList<SchemaElement>();
  }
  
  /**
   * Instantiates a list of predetermined size
   * @param e The schema element for that list
   * @param size The size of the list
   */
  public ListElement(SchemaElement e, int size)
  {
    this();
    m_elementType = e;
    for (int i = 0; i < size; i++)
    {
      m_contents.add(m_elementType.copy());
    }
  }
  
  public void setSchema(SchemaElement e)
  {
    m_elementType = e;
  }

  @Override
  public BitSequence toBitSequence()
  {
    BitSequence out = new BitSequence();
    // First append the size of the list, on 8 bits
    try
    {
      out.addAll(new BitSequence(m_contents.size(), MAX_LENGTH_BITS));
    }
    catch (BitFormatException e)
    {
      // Do nothing
    }
    // Then append the bit sequence of all elements
    for (SchemaElement el : m_contents)
    {
      BitSequence bs = el.toBitSequence();
      out.addAll(bs);
    }
    return out;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    int read_bits = 0;
    if (bs.size() < MAX_LENGTH_BITS)
    {
      throw new ReadException();
    }
    BitSequence length = bs.truncatePrefix(8);
    read_bits += MAX_LENGTH_BITS;
    int num_elements = length.intValue();
    for (int i = 0; i < num_elements; i++)
    {
      SchemaElement new_el = m_elementType.copy();
      int read = new_el.fromBitSequence(bs);
      m_contents.add(new_el);
      read_bits += read;
    }
    return read_bits;
  }

  @Override
  public SchemaElement copy()
  {
    ListElement out = new ListElement();
    out.m_elementType = m_elementType.copy();
    for (int i = 0; i < m_contents.size(); i++)
    {
      out.m_contents.add(m_contents.get(i).copy());
    }
    return out;
  }

  @Override
  public void put(String path, Object value) throws TypeMismatchException
  {
    if (path.isEmpty())
    {
      copyFrom(value);
    }
    path = path.trim();
    if (!path.startsWith("["))
    {
      // Invalid expression
      return;
    }
    int closing_bracket_pos = path.indexOf("]");
    if (closing_bracket_pos < 0)
    {
      // Invalid expression
      return;
    }
    String key_to_get = path.substring(1, closing_bracket_pos);
    if (key_to_get.isEmpty())
    {
      // TODO Empty key: signals that one is to add an element
      // to the list
    }
    int value_index = -1;
    try
    {
      value_index = Integer.parseInt(key_to_get);
    }
    catch (NumberFormatException nfe)
    {
      // Invalid index
      return;
    }
    if (value_index < 0 || value_index >= m_contents.size())
    {
      // Invalid index
      return;
    }
    path = path.substring(closing_bracket_pos + 1);
    if (path.startsWith("."))
    {
      path = path.substring(1);
    }
    SchemaElement out = m_contents.get(value_index);
    out.put(path, value);
  }

  @Override
  public SchemaElement get(String path)
  {
    if (path.isEmpty())
    {
      return this;
    }
    path = path.trim();
    if (!path.startsWith("["))
    {
      // Invalid expression
      return null;
    }
    int closing_bracket_pos = path.indexOf("]");
    if (closing_bracket_pos < 0)
    {
      // Invalid expression
      return null;
    }
    String key_to_get = path.substring(1, closing_bracket_pos);
    int value_index = -1;
    try
    {
      value_index = Integer.parseInt(key_to_get);
    }
    catch (NumberFormatException nfe)
    {
      // Invalid index
      return null;
    }
    if (value_index < 0 || value_index >= m_contents.size())
    {
      // Invalid index
      return null;
    }
    path = path.substring(closing_bracket_pos + 1);
    if (path.startsWith("."))
    {
      path = path.substring(1);
    }
    SchemaElement out = m_contents.get(value_index);
    return out.get(path);
  }
  
  protected void copyFrom(Object value) throws TypeMismatchException
  {
    if (!(value instanceof ListElement))
    {
      // Invalid element
      throw new TypeMismatchException();
    }
    m_contents.clear();
    ListElement copy_from = (ListElement) value;
    m_elementType = copy_from.m_elementType.copy();
    for (SchemaElement se : copy_from.m_contents)
    {
      m_contents.add(se.copy());
    }
  }

  @Override
  protected String toString(String indent)
  {
    StringBuilder out = new StringBuilder();
    out.append("[\n");
    int i = 0;
    for (SchemaElement value : m_contents)
    {
      if (i > 0)
      {
        out.append(",\n");
      }
      i++;
      out.append(indent).append("  ");
      // out.append(i).append(" : "); // To show indices in table
      out.append(value.toString(indent + "  "));
    }
    out.append("\n");
    out.append(indent).append("]");
    return out.toString();
  }

  @Override
  protected String schemaToString(String indent)
  {
    StringBuilder out = new StringBuilder();
    out.append("List [");
    out.append(m_elementType.schemaToString(indent + " "));
    out.append("]");
    return out.toString();
  }
  
  @Override
  public BitSequence schemaToBitSequence()
  {
    BitSequence out = null;
    try
    {
      // Write element type
      out = new BitSequence(SCHEMA_LIST, SCHEMA_WIDTH);
      // Write number of elements
      out.addAll(new BitSequence(m_contents.size(), MAX_LENGTH_BITS));
      // Write schema of containing element
      out.addAll(m_elementType.schemaToBitSequence());
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
    // Read number of elements
    if (bs.size() < MAX_LENGTH_BITS)
    {
      throw new ReadException("Cannot read length of list");
    }
    data = bs.truncatePrefix(MAX_LENGTH_BITS);
    bits_read += MAX_LENGTH_BITS;
    int length = data.intValue();
    // Read schema of containing element
    ElementInt ei = SchemaElement.bitSequenceToSchema(bs);
    bits_read += ei.m_int;
    m_elementType = ei.m_element;
    for (int i = 0; i < length; i++)
    {
      // Initialize the list with the advertised length
      m_contents.add(m_elementType.copy());
    }
    return bits_read;
  }
  
  @Override
  protected void readSchemaFromString(MutableString s) throws ReadException
  {
    s.truncateSubstring("List".length());
    s.trim();
    if (!s.startsWith("["))
    {
      // Should not happen
      throw new ReadException("Invalid definition of a List");
    }
    int index = findMatchingClosing(s);
    if (index < 0)
    {
      throw new ReadException("Invalid definition of a List");
    }
    MutableString value_string = s.substring(1, index);
    m_elementType = SchemaElement.parseSchemaFromString(value_string);
    s.truncateSubstring(index + 1);
    return;
  }
  
  @Override
  protected void readContentsFromString(MutableString s) throws ReadException
  {
    if (!s.startsWith("["))
    {
      // Should not happen
      throw new ReadException("Error reading List");
    }
    int index = findMatchingClosing(s);
    if (index < 0)
    {
      throw new ReadException("Error reading List");
    }
    MutableString value_string = s.substring(1, index);
    value_string.trim();
    while (!value_string.isEmpty())
    {
      SchemaElement se = m_elementType.copy();
      se.readContentsFromString(value_string);
      m_contents.add(se);
      value_string.trim();
      if (value_string.startsWith(","))
      {
        value_string.truncateSubstring(1);
        value_string.trim();
      }
    }
    s.truncateSubstring(index + 1);
  }
}
