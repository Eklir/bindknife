package com.github.eklir.androidprocessor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.eklir.android.bindknife.BindKnife;
import com.github.eklir.androidproccessor.bindknife.annotation.AutoBind;

public class MainActivity extends Activity {

    @AutoBind(R.id.tvTest)
    TextView textView;
    @AutoBind(R.id.ivTest)
    ImageView ivTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //执行入口
        BindKnife.inject(this);
        textView.setText("我是注入的哈哈哈");
        ivTest.setImageResource(R.mipmap.ic_launcher);
    }
}
