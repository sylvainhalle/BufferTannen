package ca.uqac.lif.buffertannen.message;

import org.junit.Test;

import ca.uqac.lif.buffertannen.message.ReadException;
import ca.uqac.lif.buffertannen.message.SchemaElement;
import ca.uqac.lif.util.FileHelper;

public class SchemaParseTest
{

	@Test
	public void testMap1() throws ReadException
	{
		String file_contents = "";
		SchemaElement se = null;
		file_contents = FileHelper.internalFileToString(SchemaParseTest.class, "data/Schema-1.txt");
		se = SchemaElement.parseSchemaFromString(file_contents);  
		System.out.println(se.schemaToString());
	}

	@Test
	public void testMap2() throws ReadException
	{
		String file_contents = "";
		SchemaElement se = null;
		file_contents = FileHelper.internalFileToString(SchemaParseTest.class, "data/Schema-2.txt");
		se = SchemaElement.parseSchemaFromString(file_contents);
		System.out.println(se.schemaToString());
	}

	@Test
	public void testList1() throws ReadException
	{
		String file_contents = "";
		SchemaElement se = null;
		file_contents = FileHelper.internalFileToString(SchemaParseTest.class, "data/Schema-3.txt");
		se = SchemaElement.parseSchemaFromString(file_contents);
		System.out.println(se.schemaToString());
	}

}
