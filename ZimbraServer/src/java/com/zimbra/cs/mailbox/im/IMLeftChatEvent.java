package com.zimbra.cs.mailbox.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class IMLeftChatEvent implements IMEvent, IMNotification {

    IMAddr mFromAddr;
    String mThreadId;
    List<IMAddr> mTargets;
    
    IMLeftChatEvent(IMAddr from, String threadId, List<IMAddr> targets) {
        mFromAddr = from;
        mThreadId = threadId;
        mTargets = targets;
    }
            
    public void run() throws ServiceException {
        for (IMAddr addr : mTargets) {
            Mailbox mbox = IMRouter.getInstance().getMailboxFromAddr(addr);
            synchronized (mbox) {
                IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);
                persona.handleLeftChat(mFromAddr, mThreadId);
                mbox.postIMNotification(this);
            }
        }
    }
        
    public String toString() {
        return new Formatter().format("IMLeftChatEvent: From: %s  Thread: %s", 
                mFromAddr, mThreadId).toString();
    }
    
    public Element toXml(Element parent) {
        Element toRet = parent.addElement("leftchat");
        toRet.addAttribute("threadId", mThreadId);
        toRet.addAttribute("addr", mFromAddr.getAddr());
        return toRet;
    }
}
