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
package ca.uqac.info.buffertannen.protocol;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;

/*package*/ class PlaceholderSegment extends Segment
{
  public PlaceholderSegment(int seq_no)
  {
    super();
    m_sequenceNumber = seq_no;
  }
  
  @Override
  public BitSequence toBitSequence()
  {
    // Should never be called
    return null;
  }

  @Override
  public int fromBitSequence(BitSequence bs) throws ReadException
  {
    // Should never be called
    return 0;
  }

  @Override
  public int getSize()
  {
    return 0;
  }
  
  @Override
  public String toString()
  {
    StringBuilder out = new StringBuilder();
    out.append("Segment type: placeholder (not received yet)\n");
    out.append("Sequence number: ").append(m_sequenceNumber).append("\n");
    return out.toString();
  }
}
