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

import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.*;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.protocol.Receiver;

import com.google.zxing.ReaderException;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;

public class BtQrReader
{
  /**
   * Return codes
   */
  public static final int ERR_OK = 0;
  public static final int ERR_FILE_NOT_FOUND = 0;
  public static final int ERR_PARSE = 2;
  public static final int ERR_IO = 3;
  public static final int ERR_ARGUMENTS = 4;
  public static final int ERR_RUNTIME = 6;
  public static final int ERR_WRITER = 7;
  public static final int ERR_CANNOT_DECODE = 8;

  public static void main(String[] args)
  {


    // Parse command line arguments
    Options options = setupOptions();
    CommandLine c_line = setupCommandLine(args, options);
    assert c_line != null;
    
    // Get action
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();
    if (remaining_args.isEmpty())
    {
      System.err.println("ERROR: missing action");
      showUsage(options);
      System.exit(ERR_ARGUMENTS);
    }
    String action = remaining_args.get(0);
    if (action.compareToIgnoreCase("read") == 0)
    {
      read(c_line);
    }
    else if (action.compareToIgnoreCase("animate") == 0)
    {
      animate(c_line);
    }
    else
    {
      System.err.println("ERROR: invalid arguments");
      showUsage(options);
      System.exit(ERR_ARGUMENTS);
    }
  }

  /**
   * Sets up the options for the command line parser
   * @return The options
   */
  @SuppressWarnings("static-access")
  private static Options setupOptions()
  {
    Options options = new Options();
    Option opt;
    opt = OptionBuilder
        .withLongOpt("help")
        .withDescription(
            "Display command line usage")
            .create("h");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("size")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set output image size to x (default: 300)")
            .create("s");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("framerate")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set animation speed to x fps (default: 8)")
            .create("r");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("framesize")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set maximum frame size to x bits (default: 2000)")
            .create("r");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("version")
        .withDescription(
            "Show version")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("stats")
        .withDescription(
            "Generate stats to stdout")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("level")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set error correction level to x (either L, M, Q, or H, default L)")
            .create("l");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("threshold")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set binarization threshold to x ('guess', or between 0 and 255, default 128)")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("output")
        .withArgName("file")
        .hasArg()
        .withDescription(
            "Output GIF animation to file")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("verbosity")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Verbose messages with level x")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("mute")
        .withDescription(
            "Don't output decoded contents to stdout, just print stats")
            .create();
    options.addOption(opt);
    return options;
  }

  /**
   * Show the benchmark's usage
   * @param options The options created for the command line parser
   */
  private static void showUsage(Options options)
  {
    HelpFormatter hf = new HelpFormatter();
    hf.printHelp("java -jar QRTranslator.jar [read|animate] [options]", options);
  }

  /**
   * Sets up the command line parser
   * @param args The command line arguments passed to the class' {@link main}
   * method
   * @param options The command line options to be used by the parser
   * @return The object that parsed the command line parameters
   */
  private static CommandLine setupCommandLine(String[] args, Options options)
  {
    CommandLineParser parser = new PosixParser();
    CommandLine c_line = null;
    try
    {
      // parse the command line arguments
      c_line = parser.parse(options, args);
    }
    catch (ParseException exp)
    {
      // oops, something went wrong
      System.err.println("ERROR: " + exp.getMessage() + "\n");
      //HelpFormatter hf = new HelpFormatter();
      //hf.printHelp(t_gen.getAppName() + " [options]", options);
      System.exit(ERR_ARGUMENTS);
    }
    return c_line;
  }
  
  protected static void read(CommandLine c_line)
  {
    int lost_frames = 0, total_frames = 0;
    int lost_segments = 0;
    int total_messages = 0, total_size = 0;
    int binarization_threshold = 128;
    int fps = 8;
    boolean guess_threshold = false, mute = false;
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();
    
    if (c_line.hasOption("threshold"))
    {
      String th = c_line.getOptionValue("threshold");
      if (th.compareToIgnoreCase("guess") == 0)
      {
        guess_threshold = true;
      }
      else if (th.compareToIgnoreCase("histogram") == 0)
      {
        binarization_threshold = -1;
      }
      else
      {
        binarization_threshold = Integer.parseInt(c_line.getOptionValue("threshold"));
        if (binarization_threshold < 0 || binarization_threshold > 255)
        {
          System.err.println("ERROR: binarization threshold must be in the range 0-255.");
          System.exit(ERR_ARGUMENTS);
        }
      }
    }
    if (c_line.hasOption("mute"))
    {
      mute = true;
    }
    
    // Instantiate BufferTannen receiver
    Receiver recv = new Receiver();

    long start_time = System.nanoTime();
    int num_files = remaining_args.size();
    int last_good_threshold = binarization_threshold;
    for (String filename : remaining_args)
    {
      total_frames++;
      File image_to_read = new File(filename);
      String data = null;
      try
      {
        // First try to read code with last good threshold value
        data = QrReadWrite.readQrCode(new FileInputStream(new File(filename)), last_good_threshold);
      }
      catch (IOException e)
      {
        // File not found
        continue;
      }
      catch (ReaderException e)
      {
        // Cannot read code: re-estimate the best threshold value
        if (guess_threshold)
        {
          QrThresholdGuesser guess = new QrThresholdGuesser();
          guess.addImage(image_to_read);
          int suggested_threshold = guess.guessThreshold(60, 220, 10, last_good_threshold);
          if (suggested_threshold > 0)
          {
            last_good_threshold = suggested_threshold;
          }
          System.err.println("Re-estimating threshold at " + last_good_threshold);
          try
          {
            data = QrReadWrite.readQrCode(new FileInputStream(new File(filename)), binarization_threshold);
          }
          catch (IOException e1)
          {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          } catch (ReaderException e1)
          {
            // Still cannot read code: too bad
          }
        }        
      }
      if (data == null)
      {
        //System.err.println("Cannot decode frame " + (total_frames - 1));
        lost_frames++;
        continue;
      }
      BitSequence bs = new BitSequence();
      try
      {
        bs.fromBase64(data);
      } catch (BitFormatException | Base64DecodingException e)
      {
        //System.err.println("Cannot decode frame " + (total_frames - 1));
        lost_frames++;
        continue;
      }
      System.err.printf("%4d/%4d (%2d%%)     \r", total_frames, num_files, (total_frames - lost_frames) * 100 / total_frames);
      //System.out.println(bs);
      recv.putBitSequence(bs);
      SchemaElement se = recv.pollMessage();
      int lost_now = recv.getMessageLostCount();
      while (se != null)
      {
        total_messages++;
        BitSequence t_bs = null;
        try
        {
          t_bs = se.toBitSequence();
        }
        catch (BitFormatException e)
        {
          // Do nothing
        }
        total_size += t_bs.size();
        for (int i = 0; i < lost_now - lost_segments; i++)
        {
          if (!mute)
            System.out.println("This message was lost");
        }
        lost_segments = lost_now;
        if (!mute)
          System.out.println(se.toString());
        se = recv.pollMessage();
        lost_now = recv.getMessageLostCount();
      }
    }
    long end_time = System.nanoTime();
    long processing_time_ms = (end_time - start_time) / 1000000;
    int raw_bits = recv.getNumberOfRawBits();
    System.err.println("Processing results               ");
    System.err.println("----------------------------------------------------");
    System.err.println(" Link quality:       " + (total_frames - lost_frames) + "/" + total_frames + " (" + ((total_frames - lost_frames) * 100 / total_frames) + "%)");
    System.err.println(" Messages received:  " + (total_messages - recv.getMessageLostCount()) + "/" + total_messages + " (" + total_size + " bits)");
    System.err.println("   Message segments: " + recv.getNumberOfMessageSegments() + " (" + recv.getNumberOfMessageSegmentsBits() + " bits)");
    System.err.println("   Delta segments:   " + recv.getNumberOfDeltaSegments() + " (" + recv.getNumberOfDeltaSegmentsBits() + " bits)");
    System.err.println("   Schema segments:  " + recv.getNumberOfSchemaSegments() + " (" + recv.getNumberOfSchemaSegmentsBits() + " bits)");
    System.err.println(" Processing rate:    " + processing_time_ms / total_frames + " ms/frame (" + total_frames * 1000 / processing_time_ms + " fps)");
    System.err.println(" Bandwidth:");
    System.err.println("   Raw:              " + raw_bits + " bits (" + raw_bits * fps / total_frames + " bits/sec.)");
    System.err.println("   Actual:           " + recv.getNumberOfDistinctBits() + " bits (" + recv.getNumberOfDistinctBits() * fps / total_frames + " bits/sec.)");
    System.err.println("   Effective:        " + total_size + " bits (" + total_size * fps / total_frames + " bits/sec.)");
    System.err.println("----------------------------------------------------\n");
    System.exit(ERR_OK);    
  }
  
  protected static void animate(CommandLine c_line)
  {
    int image_size = 300, frame_size = 2000, fps = 30;
    String output_filename = "";
    
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();
    
    if (c_line.hasOption("size"))
    {
      image_size = Integer.parseInt(c_line.getOptionValue("size"));
    }
    if (c_line.hasOption("framesize"))
    {
      image_size = Integer.parseInt(c_line.getOptionValue("framesize"));
    }
    if (c_line.hasOption("framerate"))
    {
      fps = Integer.parseInt(c_line.getOptionValue("framerate"));
    }
    if (c_line.hasOption("output"))
    {
      output_filename = c_line.getOptionValue("output");
    }
    else
    {
      System.err.println("Output filename not specified");
      System.exit(ERR_ARGUMENTS);
    }
    
    BtQrSender animator = new BtQrSender(frame_size);
    String trace_filename = "";
    if (remaining_args.size() < 2)
    {
      System.err.println("Trace filename must be provided");
      System.exit(ERR_ARGUMENTS);
    }
    if (remaining_args.size() > 2)
    {
      // All remaining arguments are schema files
      for (int i = 2; i < remaining_args.size(); i++)
      {
        String schema_filename = remaining_args.get(i);
        try
        {
          animator.setSchema(i - 2, new File(schema_filename));
        } catch (IOException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ReadException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    trace_filename = remaining_args.get(1);
    animator.readMessages(new File(trace_filename));
    animator.animate(output_filename, 100/fps, image_size);
    
    int raw_bits = animator.m_sender.getNumberOfRawBits();
    int total_frames = animator.m_sender.getNumberOfFrames();
    int total_size = animator.m_sender.getNumberOfDeltaSegmentsBits() + animator.m_sender.getNumberOfMessageSegmentsBits();
    System.err.printf("Written to %s                  \n", output_filename);
    System.err.println("----------------------------------------------------");
    System.err.println(" Frames sent:          " + total_frames);
    System.err.println(" Messages sent (not including retransmissions):  " + (animator.m_sender.getNumberOfMessageSegments() + animator.m_sender.getNumberOfDeltaSegments()) + " (" + total_size + " bits)");
    System.err.println("   Message segments:   " + animator.m_sender.getNumberOfMessageSegments() + " (" + animator.m_sender.getNumberOfMessageSegmentsBits() + " bits, " + animator.m_sender.getNumberOfMessageSegmentsBits() / animator.m_sender.getNumberOfMessageSegments() + " bits/seg.)");
    System.err.println("   Delta segments:     " + animator.m_sender.getNumberOfDeltaSegments() + " (" + animator.m_sender.getNumberOfDeltaSegmentsBits() + " bits, " + animator.m_sender.getNumberOfDeltaSegmentsBits() / animator.m_sender.getNumberOfDeltaSegments() + " bits/seg.)");
    System.err.println("   Schema segments:    " + animator.m_sender.getNumberOfSchemaSegments() + " (" + animator.m_sender.getNumberOfSchemaSegmentsBits() + " bits, " + animator.m_sender.getNumberOfSchemaSegmentsBits() / animator.m_sender.getNumberOfSchemaSegments() + " bits/seg.)");
    System.err.println(" Bandwidth:");
    System.err.println("   Raw (with retrans.): "+ raw_bits + " bits (" + raw_bits * fps / total_frames + " bits/sec.)");
    System.err.println("   Actual:              " + total_size + " bits (" + total_size * fps / total_frames + " bits/sec.)");
    System.err.println("----------------------------------------------------\n");   
    System.exit(ERR_OK);
  }

  public static void showHeader()
  {
    System.err.println("QRTranslator");
  }  

}