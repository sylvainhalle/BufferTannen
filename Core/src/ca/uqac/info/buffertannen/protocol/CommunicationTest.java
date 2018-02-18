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

import ca.uqac.info.buffertannen.message.*;

public class CommunicationTest
{
  @SuppressWarnings("unused")
  public static void main(String[] args)
  {
    Sender sender = new Sender();
    Receiver recv = new Receiver();
    
    // Create a first schema
    FixedMapElement schema1 = new FixedMapElement();
    schema1.addToSchema("name", new SmallsciiElement());
    schema1.addToSchema("title", new SmallsciiElement());
    schema1.addToSchema("value", new IntegerElement());
    
    // Create another schema
    ListElement schema2 = new ListElement(schema1, 2);
    
    sender.setSchema(0, schema1);
    SchemaElement msg = schema1.copy();
    try
    {
      msg.put("[name]", "abc");
      msg.put("[title]", "def");
      msg.put("[value]", 32);
    }
    catch (TypeMismatchException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    sender.addMessage(0, msg);
    BitSequence f = sender.pollBitSequence();
    recv.putBitSequence(f);
    SchemaElement recvd = recv.pollMessage();
    sender.addSchemaMessage(0);
    f = sender.pollBitSequence();
    recv.putBitSequence(f);
    recvd = recv.pollMessage();
    msg = schema1.copy();
    try
    {
      msg.put("[name]", "abc");
      msg.put("[title]", "dezz");
      msg.put("[value]", 50);
    }
    catch (TypeMismatchException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    sender.addMessage(0, msg);
    BitSequence delayed_frame = sender.pollBitSequence();
    msg = schema1.copy();
    try
    {
      msg.put("[name]", "abfjfc");
      msg.put("[title]", "dezz");
      msg.put("[value]", 70);
    }
    catch (TypeMismatchException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    sender.addMessage(0, msg);
    BitSequence received_before = sender.pollBitSequence();
    recv.putBitSequence(received_before);
    recvd = recv.pollMessage(); // Should be null
    recv.putBitSequence(delayed_frame);
    recvd = recv.pollMessage(); // Should be 50
    recvd = recv.pollMessage(); // Should be 70
  }
}
