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

import ca.uqac.info.util.MutableString;

public class FixedMapElement extends SchemaElement
{
  // We use two vectors rather than a map to store key-value pairs,
  // because the ordering of the keys is important
  protected Vector<String> m_keys;
  protected Vector<SchemaElement> m_values;
  
  /**
   *  The number of bits to encode the number of keys in the map.
   *  For example, if set to 6, then the map will contain at most
   *  2<sup>6</sup> keys.
   */
  public static final int MAX_KEY_WIDTH = 6;
  
  public FixedMapElement()
  {
    super();
    m_keys = new Vector<String>();
    m_values = new Vector<SchemaElement>();
  }
  
  public void addToSchema(String key, SchemaElement type)
  {
    m_keys.add(key);
    m_values.add(type);
  }
  
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
    int value_index = m_keys.indexOf(key_to_get);
    if (value_index < 0)
    {
      // Invalid expression
      return null;      
    }
    path = path.substring(closing_bracket_pos + 1);
    if (path.startsWith("."))
    {
      path = path.substring(1);
    }
    SchemaElement out = m_values.get(value_index);
    return out.get(path);
  }

  @Override
  public BitSequence toBitSequence(boolean as_delta)
  {
    BitSequence out = new BitSequence();
    if (as_delta)
    {
      // Send a single 1 bit, indicating a change
      out.add(true);
    }
    for (int i = 0; i < m_keys.size(); i++)
    {
      SchemaElement value = m_values.get(i);
      out.addAll(value.toBitSequence());
    }
    return out;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    int bits_read = 0;
    for (int i = 0; i < m_keys.size(); i++)
    {
      SchemaElement value = m_values.get(i);
      int read = value.fromBitSequence(bs);
      bits_read += read;
      //bs.truncatePrefix(read);
    }
    return bits_read;
  }
  
  public static void main(String[] args)
  {
    FixedMapElement fme = new FixedMapElement();
    FixedMapElement fme_inside = new FixedMapElement();
    fme_inside.addToSchema("name", new SmallsciiElement());
    fme_inside.addToSchema("type", new SmallsciiElement());
    fme_inside.addToSchema("whatever", new SmallsciiElement());
    fme.addToSchema("objects", fme_inside);
    fme.addToSchema("title", new SmallsciiElement());
    FixedMapElement instance = (FixedMapElement) fme.copy();
    try
    {
      instance.put("[objects][name]", "abc");
      instance.put("[objects][type]", "1");
      instance.put("[objects][whatever]", "z");
      instance.put("[title]", "hello");
    }
    catch (TypeMismatchException te)
    {
      // Do nothing
    }
    BitSequence bs = instance.toBitSequence();
    System.out.println(bs.toString(6));
    FixedMapElement instance2 = (FixedMapElement) fme.copy();
    try
    {
      instance2.fromBitSequence(bs);
    }
    catch (ReadException e)
    {
      e.printStackTrace();
    }
    SchemaElement name = instance2.get("[objects][whatever]");
    System.out.println(name);
    
    BitSequence bit_seq = fme.schemaToBitSequence();
    try
    {
      @SuppressWarnings("unused")
      ElementInt ei = SchemaElement.bitSequenceToSchema(bit_seq);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public SchemaElement copy()
  {
    FixedMapElement out = new FixedMapElement();
    for (int i = 0; i < m_keys.size(); i++)
    {
      out.m_keys.add(m_keys.get(i));
      out.m_values.add(m_values.get(i).copy());
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
    int value_index = m_keys.indexOf(key_to_get);
    if (value_index < 0)
    {
      // Invalid expression
      return;      
    }
    path = path.substring(closing_bracket_pos + 1);
    if (path.startsWith("."))
    {
      path = path.substring(1);
    }
    SchemaElement out = m_values.get(value_index);
    out.put(path, value);
  }
  
  protected void copyFrom(Object value) throws TypeMismatchException
  {
    if (!(value instanceof FixedMapElement))
    {
      // Invalid element
      throw new TypeMismatchException();
    }
    m_keys.clear();
    m_values.clear();
    FixedMapElement copy_from = (FixedMapElement) value;
    for (int i = 0; i < copy_from.m_keys.size(); i++)
    {
      m_keys.add(copy_from.m_keys.elementAt(i));
      m_values.add(copy_from.m_values.elementAt(i).copy());
    }
  }
  
  protected String toString(String indent)
  {
    StringBuilder out = new StringBuilder();
    out.append("{\n");
    for (int i = 0; i < m_keys.size(); i++)
    {
      String key = m_keys.get(i);
      SchemaElement value = m_values.get(i);
      out.append(indent).append("  ").append(key).append(" : ").append(value.toString(indent + "  "));
      if (i < m_keys.size() - 1)
      {
        out.append(",");
      }
      out.append("\n");
    }
    out.append(indent).append("}");
    return out.toString();
  }
  
  protected String schemaToString(String indent)
  {
    StringBuilder out = new StringBuilder();
    out.append("FixedMap {\n");
    for (int i = 0; i < m_keys.size(); i++)
    {
      String key = m_keys.get(i);
      SchemaElement value = m_values.get(i);
      out.append(indent).append("  ").append(key).append(" : ").append(value.schemaToString(indent + "  "));
      if (i < m_keys.size() - 1)
      {
        out.append(",");
      }
      out.append("\n");
    }
    out.append(indent).append("}");
    return out.toString();
  }

  @Override
  public BitSequence schemaToBitSequence()
  {
    BitSequence out = null;
    try
    {
      // Write element type number
      out = new BitSequence(SCHEMA_MAP, SCHEMA_WIDTH);
      // Write number of keys
      out.addAll(new BitSequence(m_keys.size(), MAX_KEY_WIDTH));
      // Encode each element of the map; the key is a null-terminated string,
      // and the value encodes itself recursively
      for (int i = 0; i < m_keys.size(); i++)
      {
        String key = m_keys.get(i);
        SmallsciiElement sse_key = new SmallsciiElement(key);
        out.addAll(sse_key.toBitSequence());
        SchemaElement value = m_values.get(i);
        out.addAll(value.schemaToBitSequence());
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
    // Read number of keys
    if (bs.size() < MAX_KEY_WIDTH)
    {
      throw new ReadException("Cannot read number of keys");
    }
    data = bs.truncatePrefix(MAX_KEY_WIDTH);
    bits_read += MAX_KEY_WIDTH;
    int length = data.intValue();
    // Read schema of containing element
    for (int i = 0; i < length; i++)
    {
      // Read key name
      SmallsciiElement sse = new SmallsciiElement();
      int read = sse.fromBitSequence(bs);
      bits_read += read;
      String key_name = sse.toString();
      // Read schema of associated value
      ElementInt ei = SchemaElement.bitSequenceToSchema(bs);
      bits_read += ei.m_int;
      m_keys.add(key_name);
      m_values.add(ei.m_element);
    }
    return bits_read;
  }
  
  @Override
  protected void readSchemaFromString(MutableString s) throws ReadException
  {
    s.truncateSubstring("FixedMap".length());
    s.trim();
    if (!s.startsWith("{"))
    {
      // Should not happen
      throw new ReadException("Invalid definition of a Map");
    }
    int index = findMatchingClosing(s);
    if (index < 0)
    {
      throw new ReadException("Invalid definition of a Map");
    }
    MutableString value_string = s.substring(1, index);
    value_string.trim();
    while (!value_string.isEmpty())
    {
      int colon_index = value_string.indexOf(":");
      if (colon_index < 0)
      {
        throw new ReadException("Invalid definition of a Map");
      }
      MutableString key_part = value_string.substring(0, colon_index);
      key_part.replaceAll("\"", "");
      key_part.trim();
      value_string.truncateSubstring(colon_index + 1);
      value_string.trim();
      SchemaElement se = SchemaElement.parseSchemaFromString(value_string);
      m_keys.add(key_part.toString());
      m_values.add(se);
      value_string.trim();
      if (value_string.startsWith(","))
      {
        value_string.truncateSubstring(1);
        value_string.trim();
      }
    }
    s.truncateSubstring(index + 1);
  }
  
  @Override
  protected void readContentsFromString(MutableString s) throws ReadException
  {
    if (!s.startsWith("{"))
    {
      // Should not happen
      throw new ReadException("Error reading Map");
    }
    int index = findMatchingClosing(s);
    if (index < 0)
    {
      throw new ReadException("Error reading Map");
    }
    MutableString value_string = s.substring(1, index);
    value_string.trim();
    while (!value_string.isEmpty())
    {
      int colon_index = value_string.indexOf(":");
      if (colon_index < 0)
      {
        throw new ReadException("Error reading Map");
      }
      MutableString key_part = value_string.substring(0, colon_index);
      key_part.replaceAll("\"", "");
      key_part.trim();
      value_string.truncateSubstring(colon_index + 1);
      value_string.trim();
      int key_index = m_keys.indexOf(key_part.toString());
      if (key_index < 0)
      {
        throw new ReadException("Invalid key \"" + key_part.toString() + "\" while reading Map");
      }
      SchemaElement se = m_values.get(key_index);
      se.readContentsFromString(value_string);
      value_string.trim();
      if (value_string.startsWith(","))
      {
        value_string.truncateSubstring(1);
        value_string.trim();
      }
    }
    s.truncateSubstring(index + 1);
  }
  
  @Override
  public void readContentsFromDelta(SchemaElement reference, SchemaElement delta)
      throws ReadException
  {
    if (!(reference instanceof FixedMapElement))
    {
      throw new ReadException("Type mismatch in reference element: expected a FixedMapElement");
    }
    FixedMapElement el = (FixedMapElement) reference;
    if (delta instanceof NoChangeElement)
    {
      // No change: copy into self value of reference list
      m_values.clear();
      for (SchemaElement value : el.m_values)
      {
        SchemaElement element_copy = value.copy();
        m_values.add(element_copy);
      }
      return;
    }
    // Change: make sure that delta is of proper type
    if (!(delta instanceof FixedMapElement))
    {
      throw new ReadException("Type mismatch in delta element: expected a FixedMapElement or a no-change");
    }
    FixedMapElement del = (FixedMapElement) delta;
    // Everything OK: process each element of the list, again computing
    // difference between matching reference and delta list items
    int min_size = Math.min(el.m_values.size(), del.m_values.size());
    for (int i = 0; i < min_size; i++)
    {
      SchemaElement ref_el = el.m_values.get(i);
      SchemaElement del_el = del.m_values.get(i);
      SchemaElement element_to_add = ref_el.copy();
      element_to_add.readContentsFromDelta(ref_el, del_el);
      m_values.add(element_to_add);
    }
  }
  
  /**
   * Populates the as a difference between the element to represent,
   * and another element to be used as a reference.
   * <p>
   * In the case of a map,
   * key-value pairs from both maps are compared one by one, and their delta is
   * computed recursively. At the end of the process, if no delta was necessary
   * for any key-value pair, a {@link NoChangeElement} is returned in place of the
   * map itself. If the keys for both operands are different or do not appear
   * exactly in the same order,  a {@link TypeMismatchException} is thrown.
   * @param reference The element to use as a reference
   * @param new_one The new element
   * @return A Schema element representing the difference between reference and new_one
   * @throws TypeMismatchException Indicates that the declared keys for list
   *   elements in both arguments is not the same
   */
  protected static SchemaElement populateFromDelta(FixedMapElement reference, FixedMapElement new_one) throws TypeMismatchException, CannotComputeDeltaException
  {
    if (reference.m_keys.size() != new_one.m_keys.size())
    {
      throw new TypeMismatchException("Maps don't have the same keys");
    }
    boolean contains_a_change = false;
    FixedMapElement out = new FixedMapElement();
    for (int i = 0; i < reference.m_keys.size(); i++)
    {
      String ref_key = reference.m_keys.elementAt(i);
      String new_key = new_one.m_keys.elementAt(i);
      if (ref_key.compareTo(new_key) != 0)
      {
        throw new TypeMismatchException("Maps don't have the same keys");
      }
      SchemaElement ref_val = reference.m_values.elementAt(i);
      SchemaElement new_val = reference.m_values.elementAt(i);
      if (ref_val.getClass() != new_val.getClass())
      {
        throw new TypeMismatchException("Types for the value don't match");
      }
      SchemaElement delta_val = SchemaElement.createFromDelta(ref_val, new_val);
      if (!(delta_val instanceof NoChangeElement))
      {
        contains_a_change = true;
      }
      out.m_keys.add(ref_key);
      out.m_values.add(delta_val);
    }
    if (!contains_a_change)
    {
      return new NoChangeElement();
    }
    return out;
  }
  
}
