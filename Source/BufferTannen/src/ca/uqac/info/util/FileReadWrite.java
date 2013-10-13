package ca.uqac.info.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileReadWrite
{
  /**
   * Writes a string to a file. The method ends the program
   * if some IOException is thrown.
   * @param filename The filename to write to
   * @param contents The file's contents
   */
  public static void writeToFile(String filename, String contents)  throws IOException
  {
    OutputStreamWriter out = null;
    try
    {
      out = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
      out.write(contents);
    }
    catch (java.io.IOException e)
    {
      throw e;
    }
    finally
    {
      if (out != null)
        out.close();
    }
  }
  
  /**
   * Reads a file and puts its contents in a string
   * @param filename The name (and path) of the file to read
   * @return The file's contents, and empty string if the file
   * does not exist
   */
  public static String readFile(String filename) throws IOException
  {
    java.util.Scanner scanner = null;
    StringBuilder out = new StringBuilder();
    try
    {
      scanner = new java.util.Scanner(new java.io.FileInputStream(filename), "UTF-8");
      while (scanner.hasNextLine())
      {
        String line = scanner.nextLine();
        line = line.trim();
        if (line.isEmpty())
          continue;
        out.append(line).append(System.getProperty("line.separator"));
      }
    }
    catch (java.io.IOException e)
    {
      throw e;
    }
    finally
    {
      if (scanner != null)
        scanner.close();
    }
    return out.toString();
  }
  
  /**
   * Return the base name of a file name. For example, the
   * base name of "/home/sylvain/abcd.txt" is "abcd"
   * @param s A string containing only the filename (not the
   * previous path!)
   * @return The filename stripped of its extension
   */
  public static String baseName(String s)
  {
    return s.replaceFirst("[.][^.]+$", "");
  }
  
  /**
   * Same as above, but with f a file instead of a string containing
   * the filename
   * @param f A file to get the base name
   * @return The base name
   */
  public static String baseName(File f)
  {
    return baseName(f.getName());
  }

}
