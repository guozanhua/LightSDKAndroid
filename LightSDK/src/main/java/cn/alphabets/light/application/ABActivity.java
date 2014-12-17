package cn.alphabets.light.application;

import android.app.Activity;
import android.os.Bundle;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.Map;

import cn.alphabets.light.R;
import cn.alphabets.light.log.Logger;
import cn.alphabets.light.network.AuthJsonRequest;
import cn.alphabets.light.network.AuthMultipartRequest;
import cn.alphabets.light.network.Parameter;
import cn.alphabets.light.network.VolleyManager;
import cn.alphabets.light.ui.Dialog;
import cn.alphabets.light.ui.MaskFragment;

/**
 * Activity父类
 * Created by lin on 14/12/2.
 */
public class ABActivity extends Activity {

    /** Mask屏 */
    protected MaskFragment mask;

    /** 是否显示Mask屏，缺省为显示 */
    private boolean isShowWaiting;

    /** 网络请求错误处理监听器 */
    private Response.ErrorListener error;

    /**
     * 请求成功时的回调方法
     */
    public interface Success {
        public void onResponse(JSONObject response);
    }

    /**
     * Get请求
     * @param url url
     * @param params params
     * @param listener success callback function
     */
    public AuthJsonRequest GET(String url, Parameter params, Success listener) {
        return this.request(Request.Method.GET, url, params, listener);
    }

    public AuthJsonRequest POST(String url, Parameter params, Success listener) {
        return this.request(Request.Method.POST, url, params, listener);
    }

    public AuthJsonRequest PUT(String url, Parameter params, Success listener) {
        return this.request(Request.Method.PUT, url, params, listener);
    }

    public AuthJsonRequest DELETE(String url, Parameter params, Success listener) {
        return this.request(Request.Method.DELETE, url, params, listener);
    }

    public AuthMultipartRequest UPLOAD(String url, Parameter params, final Success listener) {
        return this.request(url, params.toHash(), listener);
    }

    /**
     * 禁止，启用Mask屏的显示
     * @param isShowWaiting 是否显示Mask
     */
    public void showWaiting(boolean isShowWaiting) {
        this.isShowWaiting = isShowWaiting;
    }

    /**
     * 显示Mask屏
     */
    private void showWaiting() {
        if (!this.isShowWaiting) {
            return;
        }

        if (this.mask.isVisible()) {
            return;
        }
        this.mask.show(getFragmentManager());
    }

    /**
     * 隐藏Mask屏
     */
    private void hideWaiting() {
        if (!this.isShowWaiting) {
            return;
        }

        if (!this.mask.isVisible()) {
            return;
        }
        this.mask.hide();
    }

    /**
     * 文件上传
     * @param url url
     * @param params params
     * @param listener listener
     */
    private AuthMultipartRequest request(String url, Map<String, Object> params, final Success listener) {

        this.showWaiting();
        AuthMultipartRequest request = VolleyManager.getMultipartRequest(url, params, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideWaiting();
                listener.onResponse(Parameter.parse(response));
            }
        }, error);

        // 设定tag
        request.setTag(this);
        return request;
    }

    /**
     * 调用网络请求，和Activity的方法相同。一种方法是可以调用activity的该方法
     * 现阶段不想和Activity发生关联，所以单独写了一套
     * @param method HTTP方法
     * @param url URL
     * @param params 请求参数
     * @param listener 请求成功
     */
    private AuthJsonRequest request(int method, String url, Parameter params, final Success listener) {

        this.showWaiting();
        AuthJsonRequest request = VolleyManager.getJsonRequest(method, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                hideWaiting();
                listener.onResponse(response);
            }
        }, error);

        // 设定tag
        request.setTag(this);
        return request;
    }

    /**
     * 网路请求错误处理
     */
    public void onRequestError(VolleyError error) {

        Logger.e(error);

        // 隐藏Mask屏，这里不判断isShowWaiting，如果自定义的Fragment代码里使用了mask，则出错时全部关闭
        if (this.mask.isVisible()) {
            this.mask.hide();
        }

        // 无法连接服务器
        if (error instanceof NoConnectionError) {
            Dialog.toast(R.string.server_error);
            return;
        }

        // 服务器错误
        if (error instanceof ServerError) {
            Dialog.toast(R.string.network_error);
            return;
        }

        // 其他错误
        Dialog.toast(R.string.unknown_error);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.isShowWaiting = true;
        this.mask = new MaskFragment();

        this.error = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onRequestError(error);
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        VolleyManager.getRequestQueue().cancelAll(this);
    }
}
