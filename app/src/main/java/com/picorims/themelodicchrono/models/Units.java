/*
MIT License

Copyright (c) 2022 Picorims

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.picorims.themelodicchrono.models;

import java.util.HashMap;

public class Units {
    /**
     * Describe the list of available units in a timestamp,
     * with their equivalent in milliseconds
     */
    public static final HashMap<String, Integer> UNITS = new HashMap<>();

    static {
        UNITS.put("h", 3_600_000);  // hours, ms /1000 /60 /60
        UNITS.put("m", 60_000);     // minutes
        UNITS.put("s", 1_000);      // seconds
        UNITS.put("ds", 100);       // deciseconds
    }
}
