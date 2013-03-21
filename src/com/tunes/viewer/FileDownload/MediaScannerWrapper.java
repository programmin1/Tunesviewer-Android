package com.tunes.viewer.FileDownload;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.util.Log;

/**
 * MediaScanner (for updating Android media db)
 */
public class MediaScannerWrapper implements MediaScannerConnectionClient {
    private MediaScannerConnection mConnection;
    private String mPath;
    private String mMimeType;

    public MediaScannerWrapper(Context ctx, String filePath, String mime){
        mPath = filePath;
        mMimeType = mime;
        mConnection = new MediaScannerConnection(ctx, this);
    }

    public void scan() {
        mConnection.connect();
    }

    public void onMediaScannerConnected() {
        mConnection.scanFile(mPath, mMimeType);
        Log.w("MediaScannerWrapper", "media file scanned: " + mPath);
    }

    public void onScanCompleted(String path, Uri uri) {
    	Log.i("Finished scan",uri.toString());
    	mConnection.disconnect();
    }
}
