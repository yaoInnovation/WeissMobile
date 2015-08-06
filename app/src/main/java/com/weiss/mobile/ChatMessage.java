package com.weiss.mobile;

import java.io.Serializable;

public class ChatMessage  implements Serializable {
	public boolean left;
	public String message;

	public ChatMessage(boolean left, String message) {
		super();
		this.left = left;
		this.message = message;
	}
}