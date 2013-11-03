/*
 * AccountAlreadyExistsException.java
 *
 * Copyright (C) 2012-2013 LucasEasedUp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.lucaseasedup.logit.account;

public class AccountAlreadyExistsException extends RuntimeException
{
    /**
     * Creates a new instance of
     * <code>AccountAlreadyExistsException</code> without detail message.
     */
    public AccountAlreadyExistsException()
    {
    }
    
    /**
     * Constructs an instance of
     * <code>AccountAlreadyExistsException</code> with the specified detail message.
     *
     * @param msg The detail message.
     */
    public AccountAlreadyExistsException(String msg)
    {
        super(msg);
    }
}