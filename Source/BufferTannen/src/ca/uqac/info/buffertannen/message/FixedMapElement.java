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
  public BitSequence toBitSequence()
  {
    BitSequence out = new BitSequence();
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
  
}
