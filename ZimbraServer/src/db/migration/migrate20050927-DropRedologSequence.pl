#!/usr/bin/perl
# 
# ***** BEGIN LICENSE BLOCK *****
# Zimbra Collaboration Suite Server
# Copyright (C) 2005, 2007, 2008, 2009, 2010, 2013 Zimbra Software, LLC.
# 
# The contents of this file are subject to the Zimbra Public License
# Version 1.4 ("License"); you may not use this file except in
# compliance with the License.  You may obtain a copy of the License at
# http://www.zimbra.com/license.
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
# ***** END LICENSE BLOCK *****
# 


use strict;
use Migrate;

Migrate::verifySchemaVersion(20);

dropRedoLogSequence();

Migrate::updateSchemaVersion(20, 21);

exit(0);

#####################

sub dropRedoLogSequence() {
    my $sql = "drop table if exists redolog_sequence;";
    Migrate::runSql($sql);
}
