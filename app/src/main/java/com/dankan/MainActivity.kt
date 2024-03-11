package com.dankan

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.net.URISyntaxException


class MainActivity : AppCompatActivity() {

    private val AC_GALLERY = 1001
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        //웹 뷰 설정
        setupWebView()

        //웹 페이지 로딩
        loadWebView()
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return super.onCreateView(parent, name, context, attrs)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.settings.run {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }

        //WebViewClient 설정
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view?.loadUrl(url)
                    return true
                }
                // 특정 URL 스킴에 대한 처리가 필요한 경우 여기에 추가
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.webViewClient = object: WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest): Boolean {
                Log.d(TAG, request.url.toString())

                if (request.url.scheme == "intent") {
                    try {
                        // Intent 생성
                        val intent = Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)

                        // 실행 가능한 앱이 있으면 앱 실행
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            Log.d(TAG, "ACTIVITY: ${intent.`package`}")
                            return true
                        }

                        // Fallback URL이 있으면 현재 웹뷰에 로딩
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl)
                            Log.d(TAG, "FALLBACK: $fallbackUrl")
                            return true
                        }

                        Log.e(TAG, "Could not parse anythings")

                    } catch (e: URISyntaxException) {
                        Log.e(TAG, "Invalid intent request", e)
                    }
                }
                return false
            }
        }


        webView.webChromeClient = object: WebChromeClient() {

            //팝업 열기
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {

                //웹뷰 만들기
                var childWebView = WebView(view.context)

                //부모 웹뷰와 동일하게 웹뷰 설정
                childWebView.run {
                    settings.run {
                        javaScriptEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                    }
                    layoutParams = view.layoutParams
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        webViewClient = view.webViewClient
                        webChromeClient = view.webChromeClient
                    }
                }

                //화면에 추가하기
                webView.addView(childWebView)

                //웹뷰 간 연동
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = childWebView
                resultMsg.sendToTarget()

                return true
            }

            //window.close()가 호출되면 앞에서 생성한 팝업 webview 닫기
            override fun onCloseWindow(window: WebView) {
                super.onCloseWindow(window)
                // 화면에서 제거
                webView.removeView(window)
            }
        }


        //WebChromeClient 설정 (로딩 상태 표시)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                // 웹 페이지 로딩 상태를 여기서 처리할 수 있음
            }
        }

        //파일 선택 이벤트를 처리할 수 있는 WebChromeClient 설정
        webView.webChromeClient = object : WebChromeClient() {
            //파일 선택 이벤트 처리
            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?
            ): Boolean {
                //파일 선택 다이얼로그를 열거나 파일 선택 이벤트를 처리
                Log.e("web", "chose file")
                this@MainActivity.filePathCallback = filePathCallback
                openFilePicker()
                return true
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AC_GALLERY && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let { uri ->
                filePathCallback?.onReceiveValue(arrayOf(uri))
                filePathCallback = null
            }
        }
    }

    private fun loadWebView() {
        //웹 뷰에 웹 페이지 로드
        webView.loadUrl("https://dankan-kr.web.app/")
    }

    override fun onBackPressed() {
        //뒤로가기 버튼을 눌렀을 때 웹 뷰에서 뒤로가기가 가능하면 처리, 아니면 앱 종료
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_INTERNET) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestFilePermissions()
            } else {
            }
        } else if (requestCode == PERMISSION_REQUEST_FILE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupWebView()
                loadWebView()
            } else {
            }
        }
    }

    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val clipData = data?.clipData
                val uris = mutableListOf<Uri>()
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        uris.add(uri)
                    }
                } else {
                    val uri = data?.data
                    uri?.let { uris.add(it) }
                }
                filePathCallback?.onReceiveValue(uris.toTypedArray())
                filePathCallback = null
            }
        }

    private fun openFilePicker() {
        val filePickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerIntent.type = "*/*"
        filePickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        filePickerLauncher.launch(filePickerIntent)
    }

    private fun openGallery(){
        //갤러리 오픈 인텐트 지정
        /*val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)*/
        val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        //갤러리 오픈. 반환코드 RC_GALLERY
        galleryIntent.type = "*/*"
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(galleryIntent, AC_GALLERY)
    }

    private fun requestFilePermissions() {
        //파일 액세스 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), PERMISSION_REQUEST_FILE)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_INTERNET = 101
        private const val PERMISSION_REQUEST_FILE = 102
    }

}
