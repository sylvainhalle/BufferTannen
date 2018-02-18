package ca.uqac.lif.buffertannen.message;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import ca.uqac.lif.buffertannen.message.ReadException;
import ca.uqac.lif.buffertannen.message.SchemaElement;
import ca.uqac.lif.util.FileReadWrite;

public class SchemaParseTest
{

  @Test
  public void testMap1()
  {
    String file_contents = "";
    SchemaElement se = null;
    try
    {
      file_contents = FileReadWrite.readFile("test/Schema-1.txt");
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try
    {
      se = SchemaElement.parseSchemaFromString(file_contents);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }   
    System.out.println(se.schemaToString());
  }
  
  @Test
  public void testMap2()
  {
    String file_contents = "";
    SchemaElement se = null;
    try
    {
      file_contents = FileReadWrite.readFile("test/Schema-2.txt");
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try
    {
      se = SchemaElement.parseSchemaFromString(file_contents);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }
    System.out.println(se.schemaToString());
  }
  
  @Test
  public void testList1()
  {
    String file_contents = "";
    SchemaElement se = null;
    try
    {
      file_contents = FileReadWrite.readFile("test/Schema-3.txt");
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try
    {
      se = SchemaElement.parseSchemaFromString(file_contents);
    } catch (ReadException e)
    {
      // TODO Auto-generated catch block
      fail(e.getMessage());
    }
    System.out.println(se.schemaToString());
  }

}
