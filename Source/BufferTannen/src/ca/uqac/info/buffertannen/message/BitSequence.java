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

/**
 * Representation of a sequence of bits. This sequence can be converted
 * to/from an array of bytes.
 * 
 * @author sylvain
 *
 */
public class BitSequence extends Vector<Boolean>
{

  /**
   * Dummy UID
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Empty constructor. Constructs a bit sequence of length 0.
   */
  public BitSequence()
  {
    super();
  }
  
  /**
   * Constructs a bit sequence from an array of bytes. Since
   * the array of bytes may represent a sequence of bits that is
   * not a multiple of 8, the length of the bit sequence is also
   * provided.
   * 
   * @param array The array of bytes
   * @param length The length (in <em>bits</em> of the bit sequence
   *   contained in the array of bytes
   */
  public BitSequence(byte[] array, int length) throws BitFormatException
  {
    this();
    int num_bytes = (int) Math.ceil(((float) length) / 8f);
    int cur_length = 0;
    if (num_bytes > array.length)
    {
      // Error: length is longer than the length of the array
      throw new BitFormatException();
    }
    for (int i = 0; i < num_bytes && cur_length < length; i++)
    {
      byte b = array[i];
      for (int j = 7; j >= 0 && cur_length < length; j--)
      {
        int val = (b >> j) & 1;
        if (val == 1)
        {
          this.add(true);
        }
        else
        {
          this.add(false);
        }
        cur_length++;
      }
    }
  }
  
  /**
   * Constructs a bit sequence from an integer value. The value is converted
   * to a left-padded binary sequence
   * @param value The value to initialise the bit sequence from
   * @param length The length of the bit sequence
   * @throws FormatException Thrown if length is too short to accommodate
   *   the numerical value
   */
  public BitSequence(int value, int length) throws BitFormatException
  {
    this();
    int power_length = (int) Math.pow(2, length);
    if (value > power_length)
    {
      // Error: length is too short to accommodate value
      throw new BitFormatException();
    }
    for (int i = 1; i < power_length; i *= 2)
    {
      int b = value & i;
      if (b == 0)
      {
        this.insertElementAt(false, 0);
      }
      else
      {
        this.insertElementAt(true, 0);
        value -= b;
      }
    }
  }
  
  /**
   * Constructs a bit sequence from a string. All characters
   * other than 0 or 1 are silently ignored.
   * @param s The input string
   */
  public BitSequence(String s)
  {
    this();
    for (int i = 0; i < s.length(); i++)
    {
      String c = s.substring(i, 1);
      if (c.compareTo("0") == 0)
      {
        this.add(false);
      }
      else if (c.compareTo("1") == 0)
      {
        this.add(true);
      }
    }
  }
  
  /**
   * Displays the content of a byte as a string of 0 and 1
   * @param b The byte to display
   * @return
   */
  public static String byteToString(byte b)
  {
    byte[] a = new byte[1];
    return bytesToString(a);
  }
  
  /**
   * Displays the content of a byte array as a string of 0 and 1
   * @param b The bytes to display
   * @return
   */
  public static String bytesToString(byte[] a)
  {
    if (a == null)
      return null;
    BitSequence ba = null;
    try
    {
      ba = new BitSequence(a, 8 * a.length);
    }
    catch (BitFormatException e)
    {
      return "";
    }
    return ba.toString();
  }
  
  /**
   * Outputs the sequence of bits as an array of bytes.
   * The last byte is padded with zeros if the number of bits
   * in the array is not a multiple of 8.
   * 
   * @return The array of bytes
   */
  public byte[] toByteArray()
  {
    int byte_size = (int) Math.ceil(((float) this.size()) / 8f);
    byte[] out = new byte[byte_size];
    int byte_pos = 0;
    for (int i = 0; i < this.size(); i += 8)
    {
      byte b = 0;
      for (byte j = 0; j < 8 && 8 * i + j < this.size(); j++)
      {
        if (this.elementAt(8 * i + j) == true)
        {
          b |= 1 << (7 - j);
        }
      }
      out[byte_pos] = b;
      byte_pos++;
    }
    return out;
  }
  
  /**
   * Returns the contents of a bit sequence as an integer value,
   * with the most significant bit being the first of the sequence.
   * 
   * @return The integer value
   */
  public int intValue()
  {
    int out = 0;
    int pos = 0;
    for (int i = (int) Math.pow(2, this.size() - 1); i >= 1; i /= 2)
    {
      boolean b = this.get(pos);
      if (b == true)
      {
        out += i;
      }
      pos++;
    }
    return out;
  }
  
  /**
   * Creates a bit sequence from a part of the current bit sequence.
   * @param start The start position in the sequence
   * @param length The number of bits to retrieve from the start position
   * @return The sub-sequence, or null if the bounds are invalid
   */
  public BitSequence subSequence(int start, int length)
  {
    BitSequence bs = new BitSequence();
    if (start < 0 || start + length > this.size())
    {
      // Invalid bounds
      return null;
    }
    for (int i = start; i < start + length; i++)
    {
      if (this.get(i))
      {
        bs.add(true);
      }
      else
      {
        bs.add(false);
      }
    }
    return bs;
  }
  
  /**
   * Truncates the bit sequence off the first n bits
   * @param to The number of bits to remove from the beginning of the sequence
   */
  public BitSequence truncatePrefix(int to)
  {
    BitSequence out = null;
    if (to <= 0)
    {
      // Nothing to do
      return this;
    }
    if (to >= this.size())
    {
      out = this.subSequence(0,  this.size());
      this.clear();
    }
    else
    {
      out = this.subSequence(0, to);
      this.removeRange(0, to);      
    }
    return out;
  }
  
  @Override
  public String toString()
  {
    return toString(0);
  }
  
  public String toString(int group)
  {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < this.size(); i++)
    {
      if (i > 1 && group > 0 && (i % group) == 0)
      {
        out.append(" ");
      }
      boolean b = this.get(i);
      if (b == true)
      {
        out.append("1");
      }
      else
      {
        out.append("0");
      }
    }
    return out.toString();
  }
  
  public static void main(String[] args)
  {
    BitSequence s1 = null;
    try
    {
      s1 = new BitSequence(14, 4);
    }
    catch (BitFormatException e)
    {
      e.printStackTrace();
    }
    System.out.println(s1);
    System.out.println(s1.intValue());
    byte[] b = s1.toByteArray();
    try
    {
      s1 = new BitSequence(b, 4);
    }
    catch (BitFormatException e)
    {
      e.printStackTrace();
    }  
    System.out.println(s1);
  }
}
