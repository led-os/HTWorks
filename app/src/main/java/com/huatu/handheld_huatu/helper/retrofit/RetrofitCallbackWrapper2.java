/**
 * <pre>
 * Copyright (C) 2015  校导网(武汉)科技有限责任公司 Inc！
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </pre>
 */
package com.huatu.handheld_huatu.helper.retrofit;


import com.huatu.handheld_huatu.mvpmodel.BaseResponse;

/**
 * 回调
 *
 * author : Soulwolf Create by 2015/9/12 10:47
 * email  : ToakerQin@gmail.com.
 */
public class RetrofitCallbackWrapper2<RESPONSE extends BaseResponse> extends RetrofitCallback2<RESPONSE> {

    final KindRetrofitCallBack<RESPONSE> mCallback;

    public RetrofitCallbackWrapper2(KindRetrofitCallBack<RESPONSE> callBack){
        this.mCallback = callBack;
    }

    @Override
    protected void onSuccess(RESPONSE response)  {
        if(mCallback != null&&!mCallback.isFragmentFinished()){
            mCallback.onSuccess(response);
        }
    }

    @Override
    protected void onFailure(String error,int type) {
        if(mCallback != null&&!mCallback.isFragmentFinished()){
            mCallback.onError(error, type);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if(mCallback != null){
            mCallback.onSubscriberStart();
        }
    }
}
