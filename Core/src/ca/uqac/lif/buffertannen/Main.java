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
package ca.uqac.lif.buffertannen;

public class Main 
{
	/**
	 * Version string
	 */
	protected static final String s_versionString = "0.1";
	
	public static String getVersionString()
	{
		return s_versionString;
	}
	
	public static void main(String[] args)
	{
		System.out.println("Buffer Tannen v" + s_versionString + " - Binary protocol");
		System.out.println("(C) 2013-2018 Laboratoire d'informatique formelle\nUniversité du Québec à Chicoutimi, Canada");
		System.out.println("This jar file is not meant to be run stand-alone");
	}
}
