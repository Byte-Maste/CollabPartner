package com.example.realtime_data_firebase;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class VideoPlayerDialogFragment extends DialogFragment {

    private String videoId;
    private WebView webView;
    private ProgressBar progressBar;

    public static VideoPlayerDialogFragment newInstance(String videoId) {
        VideoPlayerDialogFragment fragment = new VideoPlayerDialogFragment();
        Bundle args = new Bundle();
        args.putString("videoId", videoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        if (getArguments() != null) {
            videoId = getArguments().getString("videoId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_video_player, container, false);

        webView = view.findViewById(R.id.videoWebView);
        progressBar = view.findViewById(R.id.videoProgress);
        view.findViewById(R.id.btnCloseVideo).setOnClickListener(v -> dismiss());

        setupWebView();
        loadVideo();

        return view;
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadVideo() {
        // Use youtube-nocookie.com for privacy and less tracking
        // rel=0: show related videos from same channel (or none)
        // modestbranding=1: remove YouTube logo
        // iv_load_policy=3: hide video annotations
        String html = "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#000;'>" +
                "<iframe width='100%' height='100%' src='https://www.youtube-nocookie.com/embed/" + videoId +
                "?autoplay=1&rel=0&modestbranding=1&iv_load_policy=3&fs=0&color=white' " +
                "frameborder='0' allow='autoplay; encrypted-media' allowfullscreen style='position:fixed;top:0;left:0;width:100%;height:100%;'></iframe>"
                +
                "</body></html>";

        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "utf-8", null);
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
    }
}
