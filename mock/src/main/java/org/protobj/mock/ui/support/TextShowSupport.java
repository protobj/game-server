package org.protobj.mock.ui.support;

public interface TextShowSupport {
	default void showContents(String contents) {
		TextShowUtil.showContents(contents);
	}
	default void showContents(String contents,Object... params) {
		TextShowUtil.showContents(contents, params);
	}
}
