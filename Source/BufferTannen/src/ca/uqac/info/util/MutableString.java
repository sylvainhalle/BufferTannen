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
package ca.uqac.info.util;

public class MutableString
{
  protected String m_string;
  
  public MutableString()
  {
    super();
    m_string = "";
  }
  
  public MutableString(String s)
  {
    super();
    m_string = s;
  }
  
  public MutableString(MutableString s)
  {
    this(s.toString());
  }
  
  public void clear()
  {
    m_string = "";
  }
  
  public int indexOf(String s)
  {
    return m_string.indexOf(s);
  }
  
  public int indexOf(String s, int fromIndex)
  {
    return m_string.indexOf(s, fromIndex);
  }
  
  public void trim()
  {
    m_string = m_string.trim();
  }
  
  public int length()
  {
    return m_string.length();
  }
  
  public MutableString[] split(String regex)
  {
    String[] splitted = m_string.split(regex);
    MutableString[] out = new MutableString[splitted.length];
    for (int i = 0; i < splitted.length; i++)
    {
      out[i] = new MutableString(splitted[i]);
    }
    return out;
  }
  
  public MutableString truncateSubstring(int begin, int end)
  {
    String out = m_string.substring(begin, end);
    m_string = m_string.substring(0, begin) + m_string.substring(end);
    return new MutableString(out);
  }
  
  public MutableString truncateSubstring(int begin)
  {
    String out = m_string.substring(0, begin);
    m_string = m_string.substring(begin);
    return new MutableString(out);
  }
  
  public MutableString substring(int start, int end)
  {
    return new MutableString(m_string.substring(start, end));
  }
  
  public MutableString substring(int start)
  {
    return new MutableString(m_string.substring(start));
  }
  
  public boolean startsWith(String s)
  {
    return m_string.startsWith(s);
  }
  
  public boolean startsWithIgnoreCase(String s)
  {
    return m_string.toLowerCase().startsWith(s.toLowerCase());
  }
  
  public boolean endsWith(String s)
  {
    return m_string.endsWith(s);
  }
  
  public boolean endsWithIgnoreCase(String s)
  {
    return m_string.toLowerCase().endsWith(s.toLowerCase());
  }
  
  public boolean is(String s)
  {
    return m_string.compareTo(s) == 0;
  }
  
  public boolean isEmpty()
  {
    return m_string.isEmpty();
  }
  
  public void replaceAll(String regex, String replacement)
  {
    m_string = m_string.replaceAll(regex, replacement);
  }
  
  @Override
  public String toString()
  {
    return m_string;
  }
}
