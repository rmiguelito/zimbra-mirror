/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;

class MockMailbox extends Mailbox {

    private Account account;
    private Map<String, Metadata> metadata = new HashMap<String, Metadata>();

    MockMailbox(Account account) {
        super(new MailboxData());
        this.account = account;
    }

    @Override
    public Metadata getConfig(OperationContext octxt, String section) {
        return metadata.get(section);
    }

    @Override
    public void setConfig(OperationContext octxt, String section,
            Metadata config) {
        metadata.put(section, config);
    }

    @Override
    public Account getAccount() {
        return account;
    }

}
