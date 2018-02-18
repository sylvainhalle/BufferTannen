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
package ca.uqac.lif.buffertannen.message;

import ca.uqac.lif.util.MutableString;

/**
 * Smallscii is a set of 63 characters, intended to encode common
 * character strings at 6 bits per character. There is no character
 * corresponding to code 0, so that the null value can be safely used
 * as an end-of-string marker. 
 * @author sylvain
 *
 */
public class SmallsciiElement extends StringElement
{
  protected static String s_characters = "abcdefghijklmnopqrstuvwxyz0123456789.,!@#$%&*^()[]-+=<> |/\\'{}:";
  
  public SmallsciiElement()
  {
    super();
  }
  
  public SmallsciiElement(String s)
  {
    super(s);
  }
  
  /**
   * Returns the Smallscii code of the first symbol of the given string
   * @param s The string to look for
   * @return The code value for that symbol; -1 if outside of character set
   */
  public static int getCode(String s)
  {
    String letter = s.toLowerCase().substring(0, 1);
    return s_characters.indexOf(letter) + 1;
  }
  
  /**
   * Returns the symbol for a give Smallscii code
   * @param code The code to look for
   * @return The corresponding symbol, empty string if outside of character set
   */
  public static String getSymbol(int code)
  {
    if (code < 1 || code > 63)
      return "";
    return s_characters.substring(code - 1, code);
  }
  
  /**
   * Returns the size (in bits) of the Smallscii string
   * @return The size in bits
   */
  public int getSize()
  {
    return (m_contents.length() + 1) * 6;
  }
  
  public BitSequence toBitSequence(boolean as_delta)
  {
    BitSequence bs = new BitSequence();
    if (as_delta)
    {
      // Send a single 1 bit, indicating a change
      bs.add(true);
    }
    for (int i = 0; i < m_contents.length(); i++)
    {
      String letter = m_contents.substring(i, i+1);
      int code = getCode(letter);
      try
      {
        BitSequence n_bs = new BitSequence(code, 6);
        bs.addAll(n_bs);
      }
      catch (BitFormatException e)
      {
        // Do nothing
      }
    }
    try
    {
      // Add null value to mark end of string
      bs.addAll(new BitSequence(0, 6));
    }
    catch (BitFormatException e)
    {
      // Do nothing
    }
    return bs;
  }
  
  public int fromBitSequence(BitSequence bs, boolean as_delta) throws ReadException
  {
    StringBuilder sb = new StringBuilder();
    int bits_read = 0;
    while (bs.size() >= 6)
    {
      BitSequence subs = bs.truncatePrefix(6);
      bits_read += 6;
      int code = subs.intValue();
      if (code == 0)
      {
        break;
      }
      String letter = getSymbol(code);
      sb.append(letter);
    }
    m_contents = sb.toString();
    return bits_read;
  }
  
  public SchemaElement copy()
  {
    SmallsciiElement out = new SmallsciiElement(m_contents);
    return out;
  }
  
  public void put(String path, Object value)
  {
    if (!path.isEmpty())
    {
      return;
    }
    if (value instanceof SmallsciiElement)
    {
      SmallsciiElement copy_from = (SmallsciiElement) value;
      m_contents = copy_from.m_contents;
    }
    else if (value instanceof String)
    {
      m_contents = (String) value;
    }
  }
  
  @Override
  public BitSequence schemaToBitSequence()
  {
    BitSequence out = null;
    try
    {
      // Write element type
      out = new BitSequence(SCHEMA_SMALLSCII, SCHEMA_WIDTH);
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
    // Nothing more to read
    return 0;
  }

  @Override
  protected void readSchemaFromString(MutableString s) throws ReadException
  {
    s.truncateSubstring("Smallscii".length());
  }
  
  @Override
  protected void readContentsFromString(MutableString s) throws ReadException
  {
    if (!s.startsWith("\""))
    {
      throw new ReadException("Error reading Smallscii string");
    }
    int index = s.indexOf("\"", 1);
    if (index < 0)
    {
      throw new ReadException("Error reading Smallscii string");
    }
    MutableString value = s.truncateSubstring(index + 1);
    value.replaceAll("\"", "");
    m_contents = value.toString();
  }
  
  @Override
  protected String schemaToString(String indent)
  {
    return "Smallscii";
  }
  
  @Override
  public void readContentsFromDelta(SchemaElement reference, SchemaElement delta)
      throws ReadException
  {
    if (!(reference instanceof SmallsciiElement))
    {
      throw new ReadException("Type mismatch in reference element: expected a SmallsciiElement");
    }
    SmallsciiElement el = (SmallsciiElement) reference;
    if (delta instanceof NoChangeElement)
    {
      // No change: copy into self value of reference enum
      m_contents = new String(el.m_contents);
      return;
    }
    // Change: make sure that delta is of proper type
    if (!(delta instanceof SmallsciiElement))
    {
      throw new ReadException("Type mismatch in delta element: expected a SmallsciiElement or a no-change");
    }
    SmallsciiElement del = (SmallsciiElement) delta;
    // Everything OK: copy value of delta into self
    m_contents = del.m_contents;
  }

  /**
   * Populates the as a difference between the element to represent,
   * and another element to be used as a reference
   * @param reference The element to use as a reference
   * @param new_one The new element
   * @return A Schema element representing the difference between reference and new_one
   */
  protected static SchemaElement populateFromDelta(SmallsciiElement reference, SmallsciiElement new_one)
  {
    if (reference.m_contents.compareTo(new_one.m_contents) == 0)
    {
      return new NoChangeElement();
    }
    return new_one;
  }
}
