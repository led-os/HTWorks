package com.huatu.handheld_huatu.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.huatu.handheld_huatu.R;
import com.huatu.handheld_huatu.base.BaseFrgContainerActivity;
import com.huatu.handheld_huatu.business.me.fragment.LevelConditionFragment;
import com.huatu.handheld_huatu.utils.LogUtils;

/**
 * Created by ht on 2017/10/16.
 */

public class LevelTrpsDialog extends Dialog {
    private ImageView mTipView;
    private TextView tv_know;
    private View mContentView;
    private int statusBarHeight = 0;
    private int mLeft;
    private int mTop;
    private Activity mContext;
    DisplayMetrics dm = new DisplayMetrics();
    public LevelTrpsDialog(Activity context,int theme,int left,int top) {
        super(context,theme);
        LogUtils.d("EvaluateReportTipsDialog==>constr");
        if(context!=null) {
            this.mLeft = left;
            this.mTop = top;
            this.mContext = context;
            context.getWindowManager().getDefaultDisplay().getMetrics(dm);
            initView(context,dm.density);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.d("EvaluateReportTipsDialog==>onCreate");
        if (mContentView!=null) {
            setContentView(mContentView);
            setCanceledOnTouchOutside(true);
            setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    // TODO Auto-generated method stub
                }
            });
            WindowManager.LayoutParams p = getWindow()
                    .getAttributes();
            p.height = dm.heightPixels;
            p.width = dm.widthPixels;
            p.dimAmount = 0.0f;
            getWindow().setAttributes(p);
        }
    }

    @Override
    public void show() {
        LogUtils.d("EvaluateReportTipsDialog==>show");
        if(isActive(mContext)) {
            super.show();
        }
    }
    public static class Builder {
        private Activity mContext;
        private int mLeft;
        private int mTop;

        public Builder(Activity context) {
            this.mContext = context;
        }

        public LevelTrpsDialog.Builder setmLeft(int mLeft) {
            this.mLeft = mLeft;
            return this;
        }

        public LevelTrpsDialog.Builder setmTop(int mTop) {
            this.mTop = mTop;
            return this;
        }

        public LevelTrpsDialog create() {
            LevelTrpsDialog dialog = new LevelTrpsDialog(mContext, R.style.showrevareptipsdialog,mLeft,mTop);
            return dialog;
        }
    }


    private void initView(final Activity context, float density) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = inflater.inflate(R.layout.layout_level_tips_dialog, null);
        mTipView = (ImageView) mContentView.findViewById(R.id.eva_rep_tip_iv);
        tv_know= (TextView) mContentView.findViewById(R.id.level_tip_know_view);
        RelativeLayout.LayoutParams mgetLayoutParams = (RelativeLayout.LayoutParams) mTipView.getLayoutParams();
        mgetLayoutParams.leftMargin = mLeft-(int)density*5;
        mgetLayoutParams.topMargin = mTop - getStatusBarHeight(context)-(int)density*5;
        mTipView.setLayoutParams(mgetLayoutParams);
        mContentView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dismiss();
            }
        });
        tv_know.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseFrgContainerActivity.newInstance(mContext,
                        LevelConditionFragment.class.getName(),
                        LevelConditionFragment.getArgs());
                dismiss();
            }
        });
    }

    private int getStatusBarHeight(Context context){
        if(statusBarHeight==0){
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if(resourceId>0){
                statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return statusBarHeight;
    }
    private boolean isActive(Activity context){
        if(context!=null && !context.isFinishing()){
            return true;
        }
        return false;
    }
}
