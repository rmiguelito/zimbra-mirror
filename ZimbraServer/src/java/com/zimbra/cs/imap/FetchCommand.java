/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public class FetchCommand extends ImapCommand {

    private String sequence;
    private int attributes;
    private List<ImapPartSpecifier> parts;
    private List<ImapPartSpecifier> originalParts;

    public FetchCommand(String sequence, int attributes, List<ImapPartSpecifier> parts) {
        super();
        this.sequence = sequence;
        this.attributes = attributes;
        this.parts = parts;
        this.originalParts = (parts == null ? null : new ArrayList<ImapPartSpecifier>(parts));
    }

    @Override
    public boolean equals(Object obj) {
        FetchCommand fetch = (FetchCommand) obj;
        if (super.equals(obj)) {
            return true;
        } else if (attributes == fetch.attributes && StringUtil.equal(sequence, fetch.sequence)) {
            // special case since parts List could contain same args but
            // different order; we'll treat this as the same
            if (originalParts == null) {
                return fetch.originalParts == null;
            } else if (fetch.originalParts == null || fetch.originalParts.size() != originalParts.size()) {
                return false;
            } else {
                return originalParts.containsAll(fetch.originalParts);
            }
        } else {
            return false;
        }
    }

    @Override
    protected boolean throttle(ImapCommand lastCommand) {
        // alter parts for bug 68556, but always return false - the command can
        // be processed
        if ((attributes & ImapHandler.FETCH_FROM_MIME) == 0 && parts != null && parts.size() == 1) {
            if (parts.get(0).isIgnoredExchangeHeader()) {
                ZimbraLog.imap.warn("possible misconfigured client; requested ignored header in part %s", parts.get(0));
                parts.clear();
            }
        }
        return false;
    }

}