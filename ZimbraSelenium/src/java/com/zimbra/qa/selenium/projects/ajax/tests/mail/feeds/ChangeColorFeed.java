/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
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
package com.zimbra.qa.selenium.projects.ajax.tests.mail.feeds;

import java.net.*;

import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.items.FolderItem;
import com.zimbra.qa.selenium.framework.items.FolderItem.SystemFolder;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.PrefGroupMailByMessageTest;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DialogEditFolder;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DialogEditFolder.FolderColor;

public class ChangeColorFeed extends PrefGroupMailByMessageTest {

	public ChangeColorFeed() {
		logger.info("New " + ChangeColorFeed.class.getCanonicalName());

		
		
		

	}

	@Test(
			description = "Edit a feedfolder, change the color (Context menu -> Edit)", 
			groups = { "functional" })
	public void ChangeColorFeed_01() throws HarnessException, MalformedURLException {

		FolderItem inbox = FolderItem.importFromSOAP(app.zGetActiveAccount(), SystemFolder.Inbox);
		ZAssert.assertNotNull(inbox, "Verify the inbox is available");

		// Create a subfolder in Inbox
		String feedname = "feed" + ZimbraSeleniumProperties.getUniqueString();
		
		// feed.rss=http://server/files/Service/RSS/Basic/basic.xml
		URL feedurl = new URL(ZimbraSeleniumProperties.getStringProperty("feed.rss"));

		app.zGetActiveAccount().soapSend(
					"<CreateFolderRequest xmlns='urn:zimbraMail'>"
				+		"<folder name='"+ feedname +"' l='"+ inbox.getId() +"' url='"+ feedurl.toString() +"'/>"
				+	"</CreateFolderRequest>");

		FolderItem feed = FolderItem.importFromSOAP(app.zGetActiveAccount(), feedname);
		ZAssert.assertNotNull(feed, "Verify the subfolder is available");

		
		// Click on Get Mail to refresh the folder list
		app.zPageMail.zToolbarPressButton(Button.B_GETMAIL);

		// Rename the folder using context menu
		DialogEditFolder dialog = (DialogEditFolder) app.zTreeMail.zTreeItem(Action.A_RIGHTCLICK, Button.B_TREE_EDIT, feed);
		ZAssert.assertNotNull(dialog, "Verify the dialog opened");

		// Change the color, click OK
		dialog.zSetNewColor(FolderColor.Gray);
		dialog.zClickButton(Button.B_OK);

		// Check the color
		app.zGetActiveAccount().soapSend(
				"<GetFolderRequest xmlns='urn:zimbraMail'>"
			+		"<folder id='" + feed.getId() + "'/>"
			+	"</GetFolderRequest>");

		String color = app.zGetActiveAccount().soapSelectValue("//mail:folder[@name='" + feed.getName() + "']", "color");
		ZAssert.assertEquals(color, "8", "Verify the color of the folder is set to gray (8)");
	}

}
