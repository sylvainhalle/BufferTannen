package ca.uqac.info.buffertannen.message;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import ca.uqac.info.util.FileReadWrite;

public class MessageParseTest
{

  @Test
  public void testComplexMap()
  {
    SchemaElement se = getSchema("test/Schema-2.txt");
    String file_contents = failsafeGetFile("test/Message-1.txt");;
    try
    {
      se.readContentsFromString(file_contents);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }   
    System.out.println(se.toString());
  }
  
  @Test
  public void testSimpleList()
  {
    SchemaElement se = getSchema("test/Schema-3.txt");
    String file_contents = failsafeGetFile("test/Message-2.txt");;
    try
    {
      se.readContentsFromString(file_contents);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }   
    System.out.println(se.toString());
  }
  
  protected static SchemaElement getSchema(String filename)
  {
    String file_contents = failsafeGetFile(filename);
    SchemaElement se = null;
    try
    {
      se = SchemaElement.parseSchemaFromString(file_contents);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }
    return se;
  }
  
  protected static String failsafeGetFile(String filename)
  {
    String file_contents = "";
    try
    {
      file_contents = FileReadWrite.readFile(filename);
    } catch (IOException e)
    {
      // Do nothing
    }   
    return file_contents;
  }

}
