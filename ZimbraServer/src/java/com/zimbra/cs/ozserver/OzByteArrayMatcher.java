/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

import com.zimbra.cs.util.ZimbraLog;

public class OzByteArrayMatcher implements OzMatcher {
   
    public static final byte CR = 13;
    public static final byte LF = 10;
    public static final byte DOT = '.';
    
    public static final byte[] CRLF = new byte[]{ CR, LF };
    public static final byte[] CRLFDOTCRLF = new byte[] { CR, LF, DOT, CR, LF };

    private byte[] mMatchSequence;
    private int mMatchSequenceLength;
    private int mMatched;
    
    public OzByteArrayMatcher(byte[] endSequence) {
        mMatchSequence = endSequence;
        mMatchSequenceLength = endSequence.length;
        mMatched = 0;
    }
    
    public int match(ByteBuffer buf) {
        assert(mMatched < mMatchSequenceLength);
        boolean dump = ZimbraLog.ozserver.isDebugEnabled(); 
        StringBuffer lsb = null;
        if (dump) lsb = new StringBuffer();
        
        int n = buf.remaining();
        
        if (dump) lsb.append("remaining=" + n + " matchedAtStart=" + mMatched + " ");
        
        for (int i = 0; i < n; i++) {
            byte b = buf.get();
            
            if (dump && b >= 32 && b <=126) lsb.append("'" + (char)b + "'/");
            if (dump) lsb.append((int)b + " ");

            if (mMatchSequence[mMatched] == b) {
                mMatched++;
                if (dump) lsb.append("+" + mMatched + " ");
                if (mMatched == mMatchSequenceLength) {
                    if (dump) ZimbraLog.ozserver.debug(lsb.toString());
                    return buf.position();
                }
            } else {
                mMatched = 0; // break the match
                if (mMatchSequence[mMatched] == b) { // but now does it match start of sequence?
                    mMatched++;
                    if (dump) lsb.append("+" + mMatched + " ");
                    if (mMatched == mMatchSequenceLength) {
                        if (dump) ZimbraLog.ozserver.debug(lsb.toString());
                        return buf.position();
                    }
                }
            }
        }
        if (dump) ZimbraLog.ozserver.debug(lsb.toString());
        return -1;
    }

    public void clear() {
        mMatched = 0;
    }
    
    /**
     * Remove the terminator from the buffer
     * 
     * @param buffer
     */
    public void trim(ByteBuffer buffer) {
    	buffer.limit(buffer.limit() - mMatchSequenceLength);
    }
}
