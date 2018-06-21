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
        any();

    }

    /**
     * 在一个Publishers集合中镜像出一个首先发射的Publisher，并发射出去
     * <p>
     * <p></p>
     * <b>NOTE:</b>
     * （多个Publisher中只选择一个，即最先发射的那个Observable）
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

    /**
     * 返回一个Single,它发射一个boolen值。所有的发射数据是否满足条件
     * <em>NOTE:</em>
     * 只有所有的发射数据都满足条件时，发射true,否则发射false
     */
    private void all() {
        Disposable d = Flowable
                .just(1, 2, 3)
                .all(integer -> integer < 3)
                .subscribe(value -> LogUtils.i("all:" + value));   // all:false

    }

    /**
     * 返回一个Single,它发射一个boolen值,任意一个item满足条件，发射true
     * <em>NOTE:</em>
     * 与all相反，任意一个item满足条件时，发射true,否则发射false
     */
    private void any() {
        Disposable d = Flowable
                .just(1, 2, 3)
                .any(integer -> integer < 3)
                .subscribe(value -> LogUtils.i("any:" + value));   // any:true
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        composite.dispose();
    }
}
