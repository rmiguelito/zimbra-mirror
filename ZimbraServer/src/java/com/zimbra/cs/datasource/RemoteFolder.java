/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.Mailbox;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Collections;

class RemoteFolder {
    private final ImapConnection connection;
    private final String path;
    private final boolean uidPlus;

    private static final Log LOG = ZimbraLog.datasource;
    
    RemoteFolder(ImapConnection connection, String path) {
        this.connection = connection;
        this.path = path;
        uidPlus = connection.hasCapability(ImapCapabilities.UIDPLUS);
    }

    public void create() throws IOException {
        info("creating folder");
        try {
            connection.create(path);
        } catch (CommandFailedException e) {
            // OK if CREATE failed because mailbox already exists
            if (!exists()) throw e;
        }
    }

    public void delete() throws IOException {
        info("deleting folder");
        try {
            connection.delete(path);
        } catch (CommandFailedException e) {
            // OK if DELETE failed because mailbox didn't exist
            if (exists()) throw e;
        }
    }

    public RemoteFolder renameTo(String newName) throws IOException {
        info("renaming folder to '%s'", newName);
        connection.rename(path, newName);
        return new RemoteFolder(connection, newName);
    }

    public long appendMessage(MimeMessage msg, Flags flags, Date date)
        throws IOException {
        ensureSelected();
        ImapConfig config = connection.getImapConfig();
        File tmp = null;
        OutputStream os = null;
        try {
            tmp = File.createTempFile("lit", null, config.getLiteralDataDir());
            os = new FileOutputStream(tmp);
            msg.writeTo(os);
            os.close();
            return connection.append(path, flags, date, new Literal(tmp));
        } catch (MessagingException e) {
            throw new MailException("Error appending message", e);
        } finally {
            if (os != null) os.close();
            if (tmp != null) tmp.delete();
        }
    }

    public void deleteMessages(List<Long> uids) throws IOException {
        ensureSelected();
        int size = uids.size();
        debug("deleting %d messages(s) from folder", size);
        for (int i = 0; i < size; i += 16) {
            String seq = ImapData.asSequenceSet(
                uids.subList(i, i + Math.min(size - i, 16)));
            connection.uidStore(seq, "+FLAGS.SILENT", "(\\Deleted)");
            // If UIDPLUS supported, then expunge deleted messages
            if (uidPlus) {
                connection.uidExpunge(seq);
            }
        }
    }

    public List<Long> getUids(long startUid, long endUid) throws IOException {
        ensureSelected();
        String end = endUid > 0 ? String.valueOf(endUid) : "*";
        List<Long> uids = connection.getUids(startUid + ":" + end);
        // If sequence is "<startUid>:*" then the result will always include
        // the UID of the last message in the mailbox (see RFC 3501 6.4.8).
        // This means that if there are no UIDs >= startUid then a single UID
        // will be returned that is < startUid, so we want to make sure to
        // exclude this result.
        if (endUid <= 0 && uids.size() == 1 && uids.get(0) < startUid) {
            return Collections.emptyList();
        }
        // Sort UIDs in reverse order so we download latest messages first
        Collections.sort(uids, Collections.reverseOrder());
        return uids;
    }
    
    public boolean exists() throws IOException {
        return !connection.list("", path).isEmpty();
    }

    public void ensureSelected() throws IOException {
        if (!isSelected()) {
            select();
        }
    }
    
    public Mailbox select() throws IOException {
        return connection.select(path);
    }
    
    public boolean isSelected() {
        Mailbox mb = connection.getMailbox();
        return mb != null && mb.getName().equals(path);
    }

    public String getPath() {
        return path;
    }

    public void debug(String fmt, Object... args) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(errmsg(String.format(fmt, args)));
        }
    }

    public void info(String fmt, Object... args) {
        LOG.info(errmsg(String.format(fmt, args)));
    }

    public void warn(String msg, Throwable e) {
        LOG.error(errmsg(msg), e);
    }

    public void error(String msg, Throwable e) {
        LOG.error(errmsg(msg), e);
    }

    private String errmsg(String s) {
        return String.format("Remote folder '%s': %s", getPath(), s);
    }
}
