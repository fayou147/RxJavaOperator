package com.fy.operator.item;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiPredicate;
import io.reactivex.schedulers.Schedulers;

public class FilterOperatorActivity extends AppCompatActivity {
    private CompositeDisposable composite = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_operator);
        timeout();

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
        composite.add(d);
    }

    /**
     * 去重
     */
    private void distinct() {
        Disposable d = Flowable.just("a", "b", "c", "c", "c", "b", "b", "a", "e")
                .distinct()
                .subscribe(value -> LogUtils.i("distinct:" + value)); //a,b,c,e
    }

    /**
     * 它只判定一个数据和它的直接前驱是否是不同的(相邻两个数据是否一样)
     */
    private void distinctUntilChanged() {
        Disposable d = Flowable.just("a", "b", "c", "c", "c", "b", "b", "a", "e")
                .distinctUntilChanged(new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String s, String s2) throws Exception {
                        return s.equals(s2);
                    }
                })
                .subscribe(value -> LogUtils.w("distinctUntilChanged:" + value)); //a,b,c,b,a,e
        composite.add(d);
    }

    /**
     * 返回满足过滤条件的数据
     */
    private void filter() {
        Disposable d = Flowable.range(1, Flowable.bufferSize() * 2)
                .filter(integer -> integer > 100)
                .subscribe(value -> LogUtils.i("filter:" + value));
    }

    /**
     * 限制上游项目的数量和从上游请求的总下游请求量，防止上游创建多余项目。
     */
    private void limit() {
        Flowable
                .range(1, 100)
                .observeOn(Schedulers.computation())
                .limit(5)
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(10);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        LogUtils.e("limit:" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {
                        LogUtils.e("onComplete:");
                    }
                });
    }

    /**
     * 跳过开始的N项数据,发射剩余的
     */
    private void skip() {
        Disposable d = Flowable.range(1, 10)
                .skip(5)
                .subscribe(this::print);  //6，7，8，9，10
    }

    /**
     * 如果超过了规定的超时时间，会抛出TimeoutException或者抛出指定的Publisher
     */
    private void timeout() {
        Disposable d = Flowable.intervalRange(1, 10, 1000, 1500, TimeUnit.MILLISECONDS)
//                .timeout(1000, TimeUnit.MILLISECONDS, Flowable.just(1000l))
                .timeout(1000, TimeUnit.MILLISECONDS)
                .subscribe(this::print, this::print);  //不发射任何item,直接抛出异常
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        composite.dispose();
    }

    private <T> void print(T t) {
        LogUtils.e("#" + t.getClass().getSimpleName() + ":" + t);
    }
}
