package ca.uqac.info.buffertannen.message;

import static org.junit.Assert.*;

import org.junit.Test;

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
