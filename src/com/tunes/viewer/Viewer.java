package com.tunes.viewer;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * Subclass of WebView that handles hold-click.
 */
public class Viewer extends WebView {
	private static final int ID_COPYURLIMAGE = 0;
	private static final int ID_VIEWIMAGE = 1;
	private static final int ID_SA = 2;
	private static final int ID_SHARELINK = 3;

	public Viewer(Context context) {
		super(context);
	}
	
	public Viewer(Context context, AttributeSet atts) {
		super(context,atts);
	}
	
	protected void onCreateContextMenu(ContextMenu menu) {
		super.onCreateContextMenu(menu);
		final HitTestResult result = getHitTestResult();
		MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener()  {
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
				case ID_COPYURLIMAGE:
					ClipboardManager clip = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					clip.setText(result.getExtra());
					return true;
				case ID_VIEWIMAGE:
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(result.getExtra()));
					getContext().startActivity(intent);
					return true;
				case ID_SHARELINK:
					Intent share = new Intent(Intent.ACTION_SEND);
					share.setType("text/plain");
					share.putExtra(Intent.EXTRA_TEXT, result.getExtra());
					getContext().startActivity(Intent.createChooser(share, getContext().getString(R.string.share)));
					return true;
				}
				return true;
			}
		};
		
		if (result.getType() == HitTestResult.IMAGE_TYPE ||
				result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
			// Menu options for an image.
			//set the header title to the image url
			menu.setHeaderTitle(result.getExtra());
			menu.add(0, ID_COPYURLIMAGE, 0, "Copy Image URL").setOnMenuItemClickListener(handler);
			menu.add(0, ID_VIEWIMAGE, 0, "View Image").setOnMenuItemClickListener(handler);
		} else if (result.getType() == HitTestResult.ANCHOR_TYPE ||
			  result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
			// Menu options for a hyperlink.
			//set the header title to the link url
			menu.setHeaderTitle(result.getExtra());
			menu.add(0, ID_SHARELINK, 0, "Share Link").setOnMenuItemClickListener(handler);
		} else if (result.getType() == HitTestResult.EMAIL_TYPE) {
			Intent email = new Intent(android.content.Intent.ACTION_SEND);
			email .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"webmaster@website.com"});
			getContext().startActivity(email);
		} else if (result.getType() == HitTestResult.UNKNOWN_TYPE) {
			selectAndCopyText();
		}
	}
	
	public void selectAndCopyText() {
	    try {
	        Method m = WebView.class.getMethod("emulateShiftHeld", null);
	        m.invoke(this, null);
	    } catch (Exception e) {
	        e.printStackTrace();
	        // fallback
	        KeyEvent shiftPressEvent = new KeyEvent(0,0,
	             KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_SHIFT_LEFT,0,0);
	        shiftPressEvent.dispatch(this);
	    }
	}


}
