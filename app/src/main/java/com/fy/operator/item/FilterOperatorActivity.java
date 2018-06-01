package com.fy.operator.item;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class FilterOperatorActivity extends AppCompatActivity {
    private CompositeDisposable composite = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_operator);
        amb();
    }

    /**
     * <p></p>
     * <b>NOTE:</b>
     * 在一个Publishers集合中镜像出一个首先发射的Publisher，并发射出去
     */
    private void amb() {
        Flowable<Integer> a = Flowable.just(1, 2, 3).delay(1000, TimeUnit.MICROSECONDS);
        Flowable<Integer> b = Flowable.just(4, 5, 6);
        List<Flowable<Integer>> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        Disposable d = Flowable.amb(list).subscribe(integer -> LogUtils.i("amb:" + integer));

        // 另外的写法
        // 1
        Flowable.ambArray(a, b);
        // 2
        a.ambWith(b);
        composite.add(d);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        composite.dispose();
    }
}
