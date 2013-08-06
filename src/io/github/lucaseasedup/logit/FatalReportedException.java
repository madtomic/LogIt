/*
 * FatalReportedException.java
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
package io.github.lucaseasedup.logit;

/**
 * Exception that does not need any further logging,
 * and is a signal to stop execution immediately.
 * 
 * @author LucasEasedUp
 */
public class FatalReportedException extends ReportedException
{
    /**
     * Creates a new instance of
     * <code>FatalReportedException</code> without detail message.
     */
    public FatalReportedException()
    {
    }
    
    /**
     * Constructs an instance of
     * <code>FatalReportedException</code> with the specified detail message.
     * <p/>
     * @param msg the detail message.
     */
    public FatalReportedException(String msg)
    {
        super(msg);
    }
    
    public FatalReportedException(Throwable cause)
    {
        super(cause);
    }
}