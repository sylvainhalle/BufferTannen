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

import ca.uqac.lif.util.MutableString;

/**
 * Placeholder element used in delta-messages to indicate that some reference
 * element has not changed
 * @author sylvain
 *
 */
public class NoChangeElement extends SchemaElement
{

  @Override
  public BitSequence toBitSequence(boolean as_delta)
  {
    if (!as_delta)
    {
      // This is dubious; under normal use it should never be called
      return null;
    }
    // Send a single 0 bit, indicating no change
    return new BitSequence("0");
  }

  @Override
  public int fromBitSequence(BitSequence bs, boolean as_delta) throws ReadException
  {
    // Don't need to read anything
    return 0;
  }

  @Override
  public SchemaElement copy()
  {
    // A no-change element is by definition empty
    return new NoChangeElement();
  }

  @Override
  public void put(String path, Object value) throws TypeMismatchException
  {
    // Cannot put anything into a no-change element
    throw new TypeMismatchException("Attempted to write into a no-change element");
  }

  @Override
  public SchemaElement get(String path)
  {
    if (path.isEmpty())
    {
      // Although this is dubious, we still allow one to query
      // the no-change element if the path that leads to it is valid
      return this;
    }
    return null;
  }

  @Override
  protected String toString(String indent)
  {
    return "nochange";
  }

  @Override
  protected String schemaToString(String indent)
  {
    return "nochange";
  }

  @Override
  public BitSequence schemaToBitSequence()
  {
    // This too is dubious; under normal use it should never be called
    return new BitSequence("0");
  }

  @Override
  protected void readSchemaFromString(MutableString s) throws ReadException
  {
    // TODO Auto-generated method stub

  }

  @Override
  protected void readContentsFromString(MutableString s) throws ReadException
  {
    // TODO Auto-generated method stub

  }

  @Override
  protected int readSchemaFromBitSequence(BitSequence bs) throws ReadException
  {
    // Nothing to read
    return 0;
  }

  @Override
  public void readContentsFromDelta(SchemaElement reference, SchemaElement delta)
      throws ReadException
  {
    // Cannot put anything into a no-change element
    throw new ReadException("Attempted to read contents of a no-change element");

  }

}
