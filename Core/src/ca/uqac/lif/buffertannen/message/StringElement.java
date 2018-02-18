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

public abstract class StringElement extends SchemaElement
{
  protected String m_contents;
  
  public StringElement()
  {
    super();
    m_contents = "";
  }
  
  public StringElement(String s)
  {
    this();
    m_contents = s;
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
    StringBuilder out = new StringBuilder();
    out.append("\"").append(m_contents).append("\"");
    return out.toString();
  }
  
  public BitSequence schemaToBitSequence()
  {
    BitSequence out = null;
    try
    {
      out = new BitSequence(SCHEMA_STRING, SCHEMA_WIDTH);
    } 
    catch (BitFormatException e)
    {
      // Not supposed to happen
      assert false;
    }
    return out;
  }
}
