/*
        QR Code manipulation and event processing
        Copyright (C) 2008-2013 Sylvain Hallé

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
 */
package ca.uqac.lif.qr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import com.google.zxing.WriterException;
import com.google.zxing.datamatrix.encoder.ErrorCorrection;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import ca.uqac.info.buffertannen.message.*;
import ca.uqac.info.buffertannen.protocol.*;
import ca.uqac.info.util.FileReadWrite;

public class BtQrSender
{
  
  Sender m_sender;

  /**
   * @param args
   */
  public static void main(String[] args)
  {
    BtQrSender animator = new BtQrSender(2000);
    try
    {
      animator.setSchema(0, new File("test/Pingus-Schema-1.txt"));
      animator.setSchema(1, new File("test/Pingus-Schema-2.txt"));
    }
    catch (IOException ioe)
    {
      // TODO Auto-generated catch block
      ioe.printStackTrace();      
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    animator.readMessages(new File("test/pingu-trace.txt"));
    int fps = 10;
    animator.animate("test/Pingus.gif", 100/fps);
    
    int raw_bits = animator.m_sender.getNumberOfRawBits();
    int total_frames = animator.m_sender.getNumberOfFrames();
    int total_size = animator.m_sender.getNumberOfDeltaSegmentsBits() + animator.m_sender.getNumberOfMessageSegmentsBits();
    System.err.println("Processing results               ");
    System.err.println("----------------------------------------------------");
    System.err.println(" Frames sent:          " + total_frames);
    System.err.println(" Messages sent (not including retransmissions):  " + (animator.m_sender.getNumberOfMessageSegments() + animator.m_sender.getNumberOfDeltaSegments()) + " (" + total_size + " bits)");
    System.err.println("   Message segments:   " + animator.m_sender.getNumberOfMessageSegments() + " (" + animator.m_sender.getNumberOfMessageSegmentsBits() + " bits, " + animator.m_sender.getNumberOfMessageSegmentsBits() / animator.m_sender.getNumberOfMessageSegments() + " bits/seg.)");
    System.err.println("   Delta segments:     " + animator.m_sender.getNumberOfDeltaSegments() + " (" + animator.m_sender.getNumberOfDeltaSegmentsBits() + " bits, " + animator.m_sender.getNumberOfDeltaSegmentsBits() / animator.m_sender.getNumberOfDeltaSegments() + " bits/seg.)");
    System.err.println("   Schema segments:    " + animator.m_sender.getNumberOfSchemaSegments() + " (" + animator.m_sender.getNumberOfSchemaSegmentsBits() + " bits, " + animator.m_sender.getNumberOfSchemaSegmentsBits() / animator.m_sender.getNumberOfSchemaSegments() + " bits/seg.)");
    System.err.println(" Bandwidth:");
    System.err.println("   Raw (with retrans.): "+ raw_bits + " bits (" + raw_bits * fps / total_frames + " bits/sec.)");
    System.err.println("   Effective:           " + total_size + " bits (" + total_size * fps / total_frames + " bits/sec.)");
    System.err.println("----------------------------------------------------\n");
  }
  
  public BtQrSender(int frame_length)
  {
    super();
    m_sender = new Sender();
    m_sender.setFrameMaxLength(frame_length);
  }
  
  protected void animate(String out_filename, int frame_rate)
  {
    GifAnimator animator = new GifAnimator();
    BitSequence bs = m_sender.pollBitSequence();
    while (bs != null)
    {
      try
      {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //System.out.println("Written: " + bs.toString());
        QrReadWrite.writeQrCode(out, bs.toBase64(), 300, 300, ErrorCorrectionLevel.L);
        animator.addImage(out.toByteArray());
      } catch (WriterException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      bs = m_sender.pollBitSequence();
    }
    animator.getAnimation(out_filename, frame_rate);
  }
  
  protected void setSchema(int number, String file_contents) throws ReadException
  {
    m_sender.setSchema(number, file_contents);
  }
  
  protected void setSchema(int number, File f) throws IOException, ReadException
  {
    String contents = FileReadWrite.readFile(f);
    setSchema(number, contents);
  }
  
  protected void readMessages(File f)
  {
    try
    {
      String contents = FileReadWrite.readFile(f);
      readMessages(contents);
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  protected void readMessages(String file_contents)
  {
    String[] parts = file_contents.split("\n---\n");
    for (String part : parts)
    {
      part = part.trim();
      int first_space = part.indexOf(" ");
      if (first_space < 0)
        continue;
      String left = part.substring(0, first_space);
      part = part.substring(first_space + 1);
      int schema_number = Integer.parseInt(left);
      try
      {
        m_sender.addMessage(schema_number, part);
      } catch (ReadException e)
      {
        // Ignore if cannot read
        System.err.println("Could not add message");
        continue;
      }
      catch (UnknownSchemaException e)
      {
        System.err.println("Unknown schema");
        continue;
      }
    }
  }

}
