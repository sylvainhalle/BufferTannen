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

public class ElementDeclaration
{
  protected ListElement m_elementType;
  protected int m_minOccurrences;
  protected int m_maxOccurrences;
  
  public ElementDeclaration()
  {
    super();
    m_minOccurrences = 1;
    m_maxOccurrences = 1;
    m_elementType = null;
  }
  
  public void setSchemaElement(ListElement e)
  {
    m_elementType = e;
  }
  
  public int getMaxOccurrences()
  {
    return m_maxOccurrences;
  }
  
  public int getMinOccurrences()
  {
    return m_minOccurrences;
  }
  
  public void setMaxOccurrences(int x)
  {
    m_maxOccurrences = x;
  }
  
  public void getMinOccurrences(int x)
  {
    m_minOccurrences = x;
  }
}
