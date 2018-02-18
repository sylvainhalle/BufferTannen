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

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.lif.buffertannen.message.BitFormatException;
import ca.uqac.lif.buffertannen.message.BitSequence;

public class BitSequenceTest
{

  @Test
  public void test()
  {
    String sequence = "1001011010010001010";
    BitSequence bs = new BitSequence(sequence);
    byte[] out = bs.toByteArray();
    BitSequence bs2 = null;
    try
    {
      bs2 = new BitSequence(out, sequence.length());
    } catch (BitFormatException e)
    {
      fail("Cannot create sequence");
    }
    String bs2_out = bs2.toString();
    if (bs2_out.compareTo(sequence) != 0)
    {
      fail("Read sequence not the same");
    }
  }

}
