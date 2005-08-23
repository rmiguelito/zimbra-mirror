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

/* Generated By:JavaCC: Do not edit this line. ZimbraQueryParserConstants.java */
package com.zimbra.cs.index.queryparser;

public interface ZimbraQueryParserConstants {

  int EOF = 0;
  int AND_TOKEN = 5;
  int OR_TOKEN = 6;
  int NOT_TOKEN = 7;
  int LPAREN = 8;
  int RPAREN = 9;
  int CONTENT = 10;
  int SUBJECT = 11;
  int FROM = 12;
  int TO = 13;
  int CC = 14;
  int IN = 15;
  int HAS = 16;
  int FILENAME = 17;
  int TYPE = 18;
  int ATTACHMENT = 19;
  int IS = 20;
  int DATE = 21;
  int DAY = 22;
  int WEEK = 23;
  int MONTH = 24;
  int YEAR = 25;
  int AFTER = 26;
  int BEFORE = 27;
  int SIZE = 28;
  int BIGGER = 29;
  int BIGGER_STR = 30;
  int LARGER = 31;
  int SMALLER = 32;
  int TAG = 33;
  int MESSAGE = 34;
  int MY = 35;
  int CONV = 36;
  int CONV_COUNT = 37;
  int CONV_MINM = 38;
  int CONV_MAXM = 39;
  int CONV_START = 40;
  int CONV_END = 41;
  int AUTHOR = 42;
  int TITLE = 43;
  int KEYWORDS = 44;
  int COMPANY = 45;
  int METADATA = 46;
  int ITEM = 47;
  int PLUS = 48;
  int MINUS = 49;
  int TEXT_TOK = 50;
  int INITIAL_TERM_CHAR = 51;
  int SUBSEQUENT_TERM_CHAR = 52;
  int ERROR_BRACES_WITH_NEWLINE = 54;
  int BRACES_TOK = 55;
  int ERROR_QUOTE_WITH_NEWLINE = 57;
  int QUOTED_TOK = 58;

  int DEFAULT = 0;
  int BRACES_STATE = 1;
  int QUOTED_STATE = 2;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"{\"",
    "\"\\\"\"",
    "<AND_TOKEN>",
    "<OR_TOKEN>",
    "<NOT_TOKEN>",
    "\"(\"",
    "\")\"",
    "\"content:\"",
    "\"subject:\"",
    "\"from:\"",
    "\"to:\"",
    "\"cc:\"",
    "\"in:\"",
    "\"has:\"",
    "\"filename:\"",
    "\"type:\"",
    "\"attachment:\"",
    "\"is:\"",
    "\"date:\"",
    "\"day:\"",
    "\"week:\"",
    "\"month:\"",
    "\"year:\"",
    "\"after:\"",
    "\"before:\"",
    "\"size:\"",
    "<BIGGER>",
    "\"bigger:\"",
    "\"larger:\"",
    "\"smaller:\"",
    "\"tag:\"",
    "\"message:\"",
    "\"my:\"",
    "\"conv:\"",
    "\"conv-count:\"",
    "\"conv-minm:\"",
    "\"conv-maxm:\"",
    "\"conv-start:\"",
    "\"conv-end:\"",
    "\"author:\"",
    "\"title:\"",
    "\"keywords:\"",
    "\"company:\"",
    "\"metadata:\"",
    "\"item:\"",
    "\"+\"",
    "\"-\"",
    "<TEXT_TOK>",
    "<INITIAL_TERM_CHAR>",
    "<SUBSEQUENT_TERM_CHAR>",
    "\"}\"",
    "<ERROR_BRACES_WITH_NEWLINE>",
    "<BRACES_TOK>",
    "\"\\\"\"",
    "<ERROR_QUOTE_WITH_NEWLINE>",
    "<QUOTED_TOK>",
    "\"\\r\"",
  };

}
