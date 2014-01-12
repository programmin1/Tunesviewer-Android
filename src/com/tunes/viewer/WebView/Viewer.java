package com.tunes.viewer.WebView;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebView;

import com.tunes.viewer.R;
import com.tunes.viewer.TunesViewerActivity;

/**
 * Subclass of WebView that handles hold-click.
 */
public class Viewer extends WebView {
	private static final int ID_COPYURLIMAGE = 0;
	private static final int ID_VIEWIMAGE = 1;
	private static final int ID_SHARELINK = 3;
	private static final int ID_OPENLINK = 2;
	private static final int ID_COPY = 4;
	private TunesViewerActivity _parent;
	
	//Stop the 'double tap to zoom' toast that doesn't apply here anyway:
	private static final String PREF_FILE = "WebViewSettings";
	private static final String DOUBLE_TAP_TOAST_COUNT = "double_tap_toast_count";

	public Viewer(TunesViewerActivity context) {
		super(context);
		_parent = context;
		SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
		if (prefs.getInt(DOUBLE_TAP_TOAST_COUNT, 1) > 0) {
		    prefs.edit().putInt(DOUBLE_TAP_TOAST_COUNT, 0).commit();
		}
	}
	
	public Viewer(Context context, AttributeSet atts) {
		super(context,atts);
		SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
		if (prefs.getInt(DOUBLE_TAP_TOAST_COUNT, 1) > 0) {
		    prefs.edit().putInt(DOUBLE_TAP_TOAST_COUNT, 0).commit();
		}
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
				case ID_OPENLINK:
					Intent browser = new Intent(Intent.ACTION_VIEW,Uri.parse(result.getExtra()));
					//For some reason, it still doesn't work correctly when opening link in email, then selecting open-in-browser here.
					//It just shows the previous page, and won't let you go back
					browser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_GRANT_READ_URI_PERMISSION);
					getContext().startActivity(browser);
					return true;
				case ID_COPY:
					 selectAndCopyText();
					 return true;
				}
				return true;
			}
		};
		
		if (result.getType() == HitTestResult.IMAGE_TYPE ||
				result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) { // image
			menu.setHeaderTitle(result.getExtra());
			menu.add(0, ID_COPYURLIMAGE, 0, "Copy Image URL").setOnMenuItemClickListener(handler);
			menu.add(0, ID_VIEWIMAGE, 0, "View Image").setOnMenuItemClickListener(handler);
		} else if (result.getType() == HitTestResult.ANCHOR_TYPE ||
			  result.getType() == HitTestResult.SRC_ANCHOR_TYPE) { //link
			menu.setHeaderTitle(result.getExtra());
			menu.add(0, ID_SHARELINK, 0, "Share Link").setOnMenuItemClickListener(handler);
			menu.add(0, ID_OPENLINK, 0, "Open in Browser").setOnMenuItemClickListener(handler);
		} else if (result.getType() == HitTestResult.EMAIL_TYPE) { //email address
			Intent email = new Intent(android.content.Intent.ACTION_SEND);
			email.setType("text/plain");
			email .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{result.getExtra()});
			getContext().startActivity(email);
		} else if (result.getType() == HitTestResult.PHONE_TYPE) {
	        /*String number = "tel:"+result.getExtra().trim();
	        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number)); 
	        getContext().startActivity(callIntent);*/
		}
		menu.add(0,ID_COPY,0,"Select & Copy Text").setOnMenuItemClickListener(handler);
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
