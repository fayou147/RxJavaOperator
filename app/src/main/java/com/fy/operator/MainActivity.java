package com.fy.operator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fy.operator.item.CreateOperatorActivity;
import com.fy.operator.item.FilterOperatorActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_create)
    Button btnCreate;
    @BindView(R.id.btn_filter)
    TextView btnFilter;
    @BindView(R.id.btn_merge)
    TextView btnMerge;
    @BindView(R.id.btn_condition)
    TextView btnCondition;
    @BindView(R.id.btn_vary)
    TextView btnVary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(value = {R.id.btn_create, R.id.btn_filter, R.id.btn_merge, R.id.btn_condition, R.id.btn_vary})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_create:
                gotoActivity(CreateOperatorActivity.class);
                break;
            case R.id.btn_filter:
                gotoActivity(FilterOperatorActivity.class);
                break;
            case R.id.btn_merge:
                break;
            case R.id.btn_condition:
                break;
            case R.id.btn_vary:
                break;
            default:
                break;
        }

    }

    private void gotoActivity(Class<?> cla) {
        Intent i = new Intent(this, cla);
        startActivity(i);
    }
}
