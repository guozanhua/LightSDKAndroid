package cn.alphabets.light.network;

import android.net.Uri;
import android.widget.ImageView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Map;

import cn.alphabets.light.setting.Default;

/**
 * 单例RequestQueue管理器
 * Created by lin on 14/12/1.
 */
public class VolleyManager {

    /** Internal instance variable. */
    private static VolleyManager sInstance;

    /** The request queue. */
    private RequestQueue mRequestQueue;

    /** Volley image loader */
    private AuthImageLoader mImageLoader;

    /** Image cache implementation */
    private AuthImageLoader.ImageCache mImageCache;

    /**
     * No instances
     */
    private VolleyManager() {
    }

    /**
     * This is the initializer.
     */
    public static void init() {
        if (sInstance == null) {
            sInstance = new VolleyManager();
            sInstance.mRequestQueue = Volley.newRequestQueue(ContextManager.getInstance());
            sInstance.mImageCache = new LruImageCache();
            sInstance.mImageLoader = new AuthImageLoader(getRequestQueue(), sInstance.mImageCache);
        }
    }

    /**
     * Gets the image loader from the singleton.
     * @return The RequestQueue.
     * @throws java.lang.IllegalStateException This is thrown if init has not been called.
     */
    public static RequestQueue getRequestQueue() {
        if (sInstance == null) {
            throw new IllegalStateException("The VolleyManager must be initialized.");
        }
        return sInstance.mRequestQueue;
    }

    /**
     * Gets the image loader from the singleton.
     * @return The ImageLoader.
     * @throws java.lang.IllegalStateException This is thrown if init has not been called.
     */
    public static AuthImageLoader getImageLoader() {
        if (sInstance == null) {
            throw new IllegalStateException("The VolleyManager must be initialized.");
        }
        return sInstance.mImageLoader;
    }

    /**
     * Load image and set the image to view.
     * @param file image path
     * @param view image view instance
     */
    public static void loadImage(String file, ImageView view) {

        ImageLoader.ImageListener listener = ImageLoader.getImageListener(view,
                android.R.drawable.progress_indeterminate_horizontal,
                android.R.drawable.stat_notify_error
        );

        String uri = getURL(Default.UrlLoadFile + file, Request.Method.GET, null);
        getImageLoader().get(uri, listener);
    }

    /**
     * Load image. Callback Mode. The callback function where you can get Bitmap.
     * @param file image path
     * @param success Callback
     */
    public static void loadImage(String file, AuthImageLoader.Success success) {

        ImageLoader.ImageListener listener = ImageLoader.getImageListener(
                new ImageView(ContextManager.getInstance()),
                android.R.drawable.progress_indeterminate_horizontal,
                android.R.drawable.stat_notify_error
        );

        String uri = getURL(Default.UrlLoadFile + file, Request.Method.GET, null);
        getImageLoader().addListener(success);
        getImageLoader().get(uri, listener);
    }

    /**
     * Gets the JSON Request Instance
     * @param method Request.Method
     * @param url url
     * @param jsonRequest params
     * @param listener success
     * @param errorListener error
     * @return Request instance
     */
    public static AuthJsonRequest getJsonRequest(
            int method,
            String url,
            JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            Response.ErrorListener errorListener) {

        String uri = getURL(url, method, jsonRequest);
        AuthJsonRequest request = new AuthJsonRequest(method, uri, jsonRequest, listener, errorListener);

        // 添加到Queue
        getRequestQueue().add(request);

        // 设定超时
        request.setRetryPolicy(new DefaultRetryPolicy(
                Default.RequestTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return request;
    }

    /**
     * Gets the File Request Instance
     * @param url url
     * @param params params
     * @param listener success
     * @param errorListener error
     * @return Request instance
     */
    public static AuthMultipartRequest getMultipartRequest(
            String url,
            Map<String, Object> params,
            Response.Listener<String> listener,
            Response.ErrorListener errorListener) {

        String uri = getURL(url, Request.Method.POST, null);
        AuthMultipartRequest request = new AuthMultipartRequest(uri, params, listener, errorListener);

        // 添加到Queue
        getRequestQueue().add(request);

        // 设定超时
        request.setRetryPolicy(new DefaultRetryPolicy(
                Default.RequestTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return request;
    }

    /**
     * Gets URL
     * @param url url
     * @param method Request.Method
     * @param params params
     * @return url
     */
    private static String getURL(String url, int method, JSONObject params) {
        Uri.Builder builder = new Uri.Builder();

        // make server address
        builder.scheme(Default.Protocol).encodedAuthority(Default.Server + ":" + Default.Port).appendEncodedPath(url);

        // add csrf token
        if (method == Request.Method.POST || method == Request.Method.PUT || method == Request.Method.DELETE) {
            builder.appendQueryParameter(Default.CsrfName, SessionManager.getCsrf());
        }

        // add params
        if (method == Request.Method.GET && params != null) {
            ParameterQueryParser.parse(params, builder);
        }

        return builder.build().toString();
    }
}
