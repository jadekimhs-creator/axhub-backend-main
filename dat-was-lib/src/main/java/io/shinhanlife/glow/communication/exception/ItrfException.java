package io.shinhanlife.glow.communication.exception;

public class ItrfException extends Exception {
    public ItrfException(String msg) {
        super(msg);
    }
    public ItrfException(String msgCd, String msgPrnAttrCd, String msgCt, String anxMsgCt) {
        super(msgCd + ": " + msgCt);
    }
}
