/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2008 Zimbra, Inc.
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

/**
 * Creates an IM service controller.
 * @constructor
 * @class
 * This class is a base class for IM service controllers. The app should create only one instance
 * of a service controller, and after it is created the single instance should be accessed via
 * ZmImServiceController.INSTANCE.
 *
 */
ZmImServiceController = function() {
	if (arguments.length == 0) { return; }

	ZmImServiceController.INSTANCE = this;
}

ZmImServiceController.prototype.getPresenceOperations =
function() {
	alert('Not implemented');
};
