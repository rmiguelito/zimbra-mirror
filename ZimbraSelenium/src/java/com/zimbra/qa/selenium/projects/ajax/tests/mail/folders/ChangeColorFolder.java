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
package com.zimbra.qa.selenium.projects.ajax.tests.mail.folders;

import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.items.FolderItem;
import com.zimbra.qa.selenium.framework.items.FolderItem.SystemFolder;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.PrefGroupMailByMessageTest;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DialogEditFolder;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DialogEditFolder.FolderColor;

public class ChangeColorFolder extends PrefGroupMailByMessageTest {

	public ChangeColorFolder() {
		logger.info("New " + ChangeColorFolder.class.getCanonicalName());

		
		
		

	}

	@Test(description = "Edit a folder, change the color (Context menu -> Edit)", groups = { "functional" })
	public void ChangeColorFolder_01() throws HarnessException {

		FolderItem inbox = FolderItem.importFromSOAP(app.zGetActiveAccount(),
				SystemFolder.Inbox);
		ZAssert.assertNotNull(inbox, "Verify the inbox is available");

		// Create the subfolder
		String name1 = "folder" + ZimbraSeleniumProperties.getUniqueString();

		app.zGetActiveAccount().soapSend(
					"<CreateFolderRequest xmlns='urn:zimbraMail'>"
				+		"<folder name='" + name1 + "' l='" + inbox.getId() + "'/>"
				+	"</CreateFolderRequest>");

		FolderItem subfolder1 = FolderItem.importFromSOAP(app
				.zGetActiveAccount(), name1);
		ZAssert.assertNotNull(subfolder1, "Verify the subfolder is available");

		// Click on Get Mail to refresh the folder list
		app.zPageMail.zToolbarPressButton(Button.B_GETMAIL);

		// Rename the folder using context menu
		DialogEditFolder dialog = (DialogEditFolder) app.zTreeMail.zTreeItem(
				Action.A_RIGHTCLICK, Button.B_TREE_EDIT, subfolder1);
		ZAssert.assertNotNull(dialog, "Verify the dialog opened");

		// Change the color, click OK
		dialog.zSetNewColor(FolderColor.Gray);
		dialog.zClickButton(Button.B_OK);

		// Check the color
		app.zGetActiveAccount().soapSend(
				"<GetFolderRequest xmlns='urn:zimbraMail'>"
			+		"<folder id='" + subfolder1.getId() + "'/>"
			+	"</GetFolderRequest>");

		String color = app.zGetActiveAccount().soapSelectValue("//mail:folder[@name='" + subfolder1.getName() + "']", "color");
		ZAssert.assertEquals(color, "8", "Verify the color of the folder is set to gray (8)");
	}

}
