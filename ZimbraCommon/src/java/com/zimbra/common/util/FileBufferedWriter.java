/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013 Zimbra Software, LLC.
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
package com.zimbra.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FileBufferedWriter extends Writer {

    public static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024;
    
    private static final String CHARSET = "utf-8";

    private Writer mOut;
    private int mBufSizeBytes;
    private char[] mMemBuffer;
    private int mMemBufferOffset;
    private File mTempFile;
    private OutputStreamWriter mWriter;
    private boolean mFinished;

    public FileBufferedWriter(Writer out, int maxMemSize) {
        mOut = out;
        mBufSizeBytes = Math.max(Math.min(maxMemSize, MAX_BUFFER_SIZE), 0);
        mMemBuffer = new char[mBufSizeBytes / 2];
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            mOut.close();
        }
    }

    @Override
    public void flush() throws IOException {
        // Flushing not supported.
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        int remainingMemCapacity = mMemBuffer.length - mMemBufferOffset;
        int memCharsToWrite = Math.min(len, remainingMemCapacity);
        if (memCharsToWrite > 0) {
            System.arraycopy(cbuf, off, mMemBuffer, mMemBufferOffset, memCharsToWrite);
            mMemBufferOffset += memCharsToWrite;
        }

        int fileCharsToWrite = len - memCharsToWrite;
        if (fileCharsToWrite > 0) {
            if (mWriter == null) {
                // Create the buffer file if necessary.
                mTempFile = File.createTempFile("FileBufferedWriter", ".buf");
                boolean success = false;
                try {
                    mWriter = new OutputStreamWriter(new FileOutputStream(mTempFile), CHARSET);
                    success = true;
                } finally {
                    if (!success) {
                        mTempFile.delete();
                        mTempFile = null;
                    }
                }
            }
            mWriter.write(cbuf, off + memCharsToWrite, fileCharsToWrite);
        }
    }

    public void finish() throws IOException {
        if (!mFinished) {
            mFinished = true;
            try {
                boolean hasFile = mWriter != null;
                if (hasFile) {
                    try {
                        mWriter.close();
                    } finally {
                        mWriter = null;
                    }
                }
                if (mMemBufferOffset > 0)
                    mOut.write(mMemBuffer, 0, mMemBufferOffset);
                if (hasFile) {
                    InputStreamReader reader = new InputStreamReader(new FileInputStream(mTempFile), CHARSET);
                    try {
                        int charsRead;
                        while ((charsRead = reader.read(mMemBuffer, 0, mMemBuffer.length)) != -1) {
                            mOut.write(mMemBuffer, 0, charsRead);
                        }
                    } finally {
                        reader.close();
                    }
                }
            } finally {
                if (mTempFile != null) {
                    mTempFile.delete();
                    mTempFile = null;
                }
                mMemBuffer = null;
            }
        }
    }

}