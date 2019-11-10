package com.codencode.opencvtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class ResultActivity extends AppCompatActivity {

    static final String BASE_URL = "http://www.wolframalpha.com/input/?i=";
    String queryUrl;
    WebView wb;
    ProgressBar mProgressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mProgressBar = findViewById(R.id.result_progress_bar);
        getSupportActionBar().setTitle("Showing Result");
        queryUrl = getIntent().getStringExtra("URL");

        MyClient client = new MyClient(mProgressBar);
        wb = findViewById(R.id.result_web_view);

        wb.getSettings().setJavaScriptEnabled(true);
        wb.setWebViewClient(client);
        wb.getSettings().setAllowFileAccess(true);
        client.shouldOverrideUrlLoading(wb , BASE_URL+queryUrl);
    }

    public class MyClient extends WebViewClient{
        private ProgressBar progressBar;

        public MyClient(ProgressBar progressBar) {
            this.progressBar=progressBar;
            progressBar.setVisibility(View.VISIBLE);

        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }
    }
}
