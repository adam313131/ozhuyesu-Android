package com.ozhuyesu;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.os.Environment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebActivity extends AppCompatActivity implements View.OnClickListener {

    private ScrollAwareWebView webView;
    private ProgressBar progressBar;

    private FrameLayout mLayout;
    private long exitTime = 0;
    private Context mContext;
    private InputMethodManager manager;
    private ImageButton floatingHomeButton;
    private ImageButton floatingBackButton; // 新增：返回按钮变量
    private ImageButton floatingShareButton;
    private ImageButton floatingQrcodeButton;
    private View buttonsBackgroundView; // <--- 新增：胶囊背景的变量
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final int PRESS_BACK_EXIT_GAP = 2000;
    public static final String FILE = "file://";
    // ... 您的成员变量 (webView, mUrl, 等)
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private String pendingDownloadUrl;
    private String pendingUserAgent;
    private String pendingContentDisposition;
    private String pendingMimeType;
    private long pendingContentLength;
    private void hideFloatingButton() {
        // 检查背景是否可见，如果可见则执行隐藏动画
        if (buttonsBackgroundView != null && buttonsBackgroundView.getVisibility() == View.VISIBLE) {
            // 将所有需要隐藏的View放入一个列表
            View[] viewsToHide = {floatingHomeButton, floatingShareButton, floatingBackButton, buttonsBackgroundView, floatingQrcodeButton};

            for (View view : viewsToHide) {
                if (view != null) {
                    view.animate()
                            .translationY(view.getHeight() + 100) // 向下移动一段距离（移出屏幕）
                            .alpha(0) // 渐变为透明
                            .setDuration(300) // 动画时长300毫秒
                            .withEndAction(() -> view.setVisibility(View.GONE)) // 动画结束后设为GONE
                            .start();
                }
            }
        }
    }

    private void showFloatingButton() {
        // 检查背景是否不可见，如果不可见则执行显示动画
        if (buttonsBackgroundView != null && buttonsBackgroundView.getVisibility() != View.VISIBLE) {
            // 将所有需要显示的View放入一个列表
            View[] viewsToShow = {floatingHomeButton, floatingShareButton, floatingBackButton, buttonsBackgroundView, floatingQrcodeButton};

            for (View view : viewsToShow) {
                if (view != null) {
                    view.setVisibility(View.VISIBLE); // 先设为可见
                    view.animate()
                            .translationY(0) // 移动回原始位置
                            .alpha(1) // 渐变为不透明
                            .setDuration(300) // 动画时长300毫秒
                            .start();
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 请求窗口没有标题栏 (必须在 setContentView 之前调用)
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 设置全屏标志 (必须在 setContentView 之前调用)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 对于具有刘海或摄像孔的设备，允许内容绘制到显示切口区域
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        // 防止底部按钮上移
        getWindow().setSoftInputMode
                (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_web);
        mLayout = (FrameLayout) findViewById(R.id.fl_video);
        mContext = WebActivity.this;
        manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        initView();

        floatingHomeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (webView != null) {
                    // 清理文件缓存
                    webView.clearCache(true);
                    // 清理数据库、LocalStorage等
                    android.webkit.WebStorage.getInstance().deleteAllData();
                    webView.reload(); // 重新加载当前页面
                    Toast.makeText(mContext, "正在刷新并清理缓存", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        floatingHomeButton.setOnTouchListener(new View.OnTouchListener() {
            private long pressStartTime;
            private static final int MAX_CLICK_DURATION = 200; // 小于200毫秒的触摸被视为单击

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressStartTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        long pressDuration = System.currentTimeMillis() - pressStartTime;
                        if (pressDuration < MAX_CLICK_DURATION) {
                            if (webView != null) {
                                webView.loadUrl(getResources().getString(R.string.home_url));
                            }
                        }
                        break;
                }
                return false;
            }
        });

        floatingBackButton.setOnClickListener(v -> {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            }
        });

        floatingShareButton.setOnClickListener(v -> {
            if (webView != null && webView.getUrl() != null && !webView.getUrl().isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, webView.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                startActivity(Intent.createChooser(shareIntent, "分享"));
            } else {
                Toast.makeText(mContext, "无法获取当前页面链接", Toast.LENGTH_SHORT).show();
            }
        });

        floatingQrcodeButton.setOnClickListener(v -> showQRCodeAndHandleClick());

        initWeb();
        if (webView != null) {
            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent,
                                            String contentDisposition, String mimeType,
                                            long contentLength) {
                    Log.d("DownloadDebug", "Download started for URL: " + url);

                    pendingDownloadUrl = url;
                    pendingUserAgent = userAgent;
                    pendingContentDisposition = contentDisposition;
                    pendingMimeType = mimeType;
                    pendingContentLength = contentLength;

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        if (ContextCompat.checkSelfPermission(WebActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(WebActivity.this,
                                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                        } else {
                            startDownload();
                        }
                    } else {
                        startDownload();
                    }
                }
            });
        } else {
            Log.e("WebActivity", "WebView is null, cannot set DownloadListener.");
        }

    }

    private void showQRCodeAndHandleClick() {
        if (webView != null && webView.getUrl() != null && !webView.getUrl().isEmpty()) {
            try {
                Bitmap qrCodeBitmap = createQRCodeBitmap(webView.getUrl(), 618, 618);
                showQRCodePopup(qrCodeBitmap);
            } catch (WriterException e) {
                e.printStackTrace();
                Toast.makeText(mContext, "无法生成二维码", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "无法获取当前页面链接", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createQRCodeBitmap(String url, int width, int height) throws WriterException {
        Hashtable<EncodeHintType, String> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, width, height, hints);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private void showQRCodePopup(Bitmap qrCodeBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);
        builder.setView(dialogView);

        ImageView imageView = dialogView.findViewById(R.id.qr_code_image_view);
        imageView.setImageBitmap(qrCodeBitmap);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int dialogWidth = (int) (screenWidth * 0.75);
            window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }


    private void startDownload() {
        if (pendingDownloadUrl == null) {
            Log.e("DownloadDebug", "Pending download URL is null. Cannot start download.");
            Toast.makeText(this, "链接无效", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pendingDownloadUrl));

        String cookies = CookieManager.getInstance().getCookie(pendingDownloadUrl);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", pendingUserAgent);

        String fileName = getFileName(pendingDownloadUrl, pendingContentDisposition, pendingMimeType);
        Log.d("DownloadDebug", "Guessed FileName: " + fileName);


        request.setTitle(fileName);
        request.setDescription("下载中...");
        request.setMimeType(pendingMimeType);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            try {
                long downloadId = downloadManager.enqueue(request);
                Toast.makeText(this, "下载开始: " + fileName, Toast.LENGTH_LONG).show();
                Log.d("DownloadDebug", "Download enqueued with ID: " + downloadId);
            } catch (Exception e) {
                Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("DownloadDebug", "Error enqueuing download", e);
            }
        } else {
            Toast.makeText(this, "无法下载", Toast.LENGTH_SHORT).show();
            Log.e("DownloadDebug", "DownloadManager is null");
        }

        clearPendingDownload();
    }

    public static String getFileName(String url, String contentDisposition, String mimeType) {
        String filename = null;
        if (contentDisposition != null) {
            try {
                Pattern pattern = Pattern.compile("filename=\"?([^\";]+)\"?");
                Matcher matcher = pattern.matcher(contentDisposition.toLowerCase());
                if (matcher.find()) {
                    filename = URLDecoder.decode(matcher.group(1), "UTF-8");
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (filename == null) {
            if (url != null) {
                filename = url.substring(url.lastIndexOf('/') + 1);
                if (filename.contains("?")) {
                    filename = filename.substring(0, filename.indexOf('?'));
                }
            }
        }

        if (filename == null) {
            filename = String.valueOf(System.currentTimeMillis());
        }

        return filename;
    }


    private void clearPendingDownload() {
        pendingDownloadUrl = null;
        pendingUserAgent = null;
        pendingContentDisposition = null;
        pendingMimeType = null;
        pendingContentLength = 0;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已获取，开始下载...", Toast.LENGTH_SHORT).show();
                startDownload();
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法下载", Toast.LENGTH_LONG).show();
                clearPendingDownload();
            }
        }
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != -1) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                android.database.Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);

                    if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(statusIndex)) {
                        String fileName = cursor.getString(titleIndex);
                        Toast.makeText(WebActivity.this, "下载完成: " + fileName, Toast.LENGTH_LONG).show();
                        Log.d("DownloadDebug", "Download completed: " + fileName + " (ID: " + id + ")");
                    } else if (DownloadManager.STATUS_FAILED == cursor.getInt(statusIndex)){
                        int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int reason = cursor.getInt(reasonIndex);
                        String failedReason = "";
                        switch(reason){
                            case DownloadManager.ERROR_CANNOT_RESUME:
                                failedReason = "ERROR_CANNOT_RESUME";
                                break;
                            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                failedReason = "ERROR_DEVICE_NOT_FOUND";
                                break;
                        }
                        Log.e("DownloadDebug", "Download failed. Reason: " + failedReason + " (ID: " + id + ")");
                        Toast.makeText(WebActivity.this, "无法下载: " + failedReason, Toast.LENGTH_LONG).show();
                    }
                    cursor.close();
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        clearPendingDownload();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void initView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        floatingHomeButton = findViewById(R.id.floating_home_button);
        floatingBackButton = findViewById(R.id.floating_back_button);
        floatingShareButton = findViewById(R.id.floating_share_button);
        floatingQrcodeButton = findViewById(R.id.floating_qrcode_button);
        buttonsBackgroundView = findViewById(R.id.buttons_background_view);
    }
    @SuppressLint("SetJavaScriptEnabled")
    private void initWeb() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (failingUrl.contains("://ozhuyesu.com") && !failingUrl.contains("://ozhuyesu.jiasugo.dpdns.org")) {
                    String newUrl = failingUrl.replace("://ozhuyesu.com", "://ozhuyesu.jiasugo.dpdns.org");
                    view.loadUrl(newUrl);
                } else if (failingUrl.contains("://ozhuyesu.jiasugo.dpdns.org")) {
                    view.loadUrl("file:///android_asset/web/index.html");
                    Toast.makeText(mContext, "网络无法访问已转为离线", Toast.LENGTH_SHORT).show();
                } else if (!failingUrl.startsWith("file://")) { // to prevent loop on offline page error
                    view.loadUrl("https://ozhuyesu.com");
                    Toast.makeText(mContext, "此网页目前无法访问", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                if (url == null) {
                    return false;
                }

                if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }


        });

        webView.setOnScrollListener(new ScrollAwareWebView.OnScrollListener() {
            @Override
            public void onScrollUp() {
                showFloatingButton();
            }

            @Override
            public void onScrollDown() {
                hideFloatingButton();
            }
        });
        webView.setWebChromeClient(new MkWebChromeClient());

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setUserAgentString(settings.getUserAgentString() + " Ozhuyesu/" + getVerName(mContext));

        settings.setUseWideViewPort(true);

        settings.setLoadWithOverviewMode(true);

        settings.setSupportZoom(true);

        settings.setBuiltInZoomControls(true);

        settings.setDisplayZoomControls(false);

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        settings.setAllowFileAccess(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setLoadsImagesAutomatically(true);

        settings.setDefaultTextEncodingName("utf-8");

        if (Build.VERSION.SDK_INT >= 19) {
            settings.setLoadsImagesAutomatically(true);
        } else {
            settings.setLoadsImagesAutomatically(false);
        }

        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.loadUrl(getResources().getString(R.string.home_url));
    }

    private static String getVerName(Context context) {
        String verName = "unKnow";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }
    private class MkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url == null) {
                return true;
            }

            if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
                view.loadUrl(url);
                return true;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);


        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.INVISIBLE);
            if(!webView.getSettings().getLoadsImagesAutomatically()) {
                webView.getSettings().setLoadsImagesAutomatically(true);
            }
        }
    }

    private class MkWebChromeClient extends WebChromeClient {
        private final static int WEB_PROGRESS_MAX = 100;
        private CustomViewCallback mCustomViewCallback;

        private View mCustomView;

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);

            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }

            mCustomView = view;
            mCustomView.setVisibility(View.VISIBLE);
            mCustomViewCallback = callback;
            mLayout.addView(mCustomView);
            mLayout.setVisibility(View.VISIBLE);
            mLayout.bringToFront();

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (mCustomView == null) {
                return;
            }
            mCustomView.setVisibility(View.GONE);
            mLayout.removeView(mCustomView);
            mCustomView = null;
            mLayout.setVisibility(View.GONE);;
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Exception e) {
            }

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            progressBar.setProgress(newProgress);
            if (newProgress > 0) {
                if (newProgress == WEB_PROGRESS_MAX) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
