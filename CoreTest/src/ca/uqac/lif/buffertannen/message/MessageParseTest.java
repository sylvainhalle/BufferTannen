package ca.uqac.lif.buffertannen.message;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.lif.buffertannen.message.ReadException;
import ca.uqac.lif.buffertannen.message.SchemaElement;
import ca.uqac.lif.util.FileHelper;

public class MessageParseTest
{

  @Test
  public void testComplexMap() throws ReadException
  {
    SchemaElement se = getSchema("data/Schema-2.txt");
    String file_contents = FileHelper.internalFileToString(this.getClass(), "data/Message-1.txt");
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
  public void testSimpleList() throws ReadException
  {
    SchemaElement se = getSchema("data/Schema-3.txt");
    String file_contents = FileHelper.internalFileToString(this.getClass(), "data/Message-2.txt");;
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
  
  protected static SchemaElement getSchema(String filename) throws ReadException
  {
    String file_contents = FileHelper.internalFileToString(MessageParseTest.class, filename);
    return SchemaElement.parseSchemaFromString(file_contents);
  }

}
