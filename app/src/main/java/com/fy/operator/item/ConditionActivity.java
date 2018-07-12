package com.fy.operator.item;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.LongConsumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class ConditionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_condition);
        throttleLatest();
    }

    /**
     * 延迟多少时间后发射
     */
    private void delay() {
        LogUtils.i("<--------开始时间");
        Disposable d = Flowable.just(1, 2, 3)
                .delay(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> LogUtils.i("delay:" + value));
    }

    private void doxxx() {
        Flowable.range(1, 5)
                .doOnTerminate(() -> {
                    //注册一个动作，Observable终止(onComplete or onError)之前会被调用，
                    LogUtils.i("doOnTerminate");
                })
                .doFinally(() -> {
                    //注册一个动作,当Observable终止(onComplete or onError)之后会被调用。
                    LogUtils.i("doFinally");
                })
                .doAfterTerminate(() -> {
                    //终止(onComplete or onError)之后，doFinally之前
                    LogUtils.i("doAfterTerminate");
                })
                .doAfterNext(integer -> {
                    //每次调用onNext之后调用
                    LogUtils.w("doAfterNext:" + integer);
                })
                .doOnLifecycle(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription subscription) throws Exception {
                        //订阅之前调用
                        LogUtils.d("OnSubscribe");
                    }
                }, new LongConsumer() {
                    @Override
                    public void accept(long t) throws Exception {
                        //t:5  下游请求的数量
                        LogUtils.d("OnRequest:" + t);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        LogUtils.d("OnCancel");
                    }
                })
                .doOnRequest(t -> {
                    LogUtils.w("doOnRequest:" + t);
                })
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(5);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        LogUtils.i("onNext:" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LogUtils.i("onError:" + t);
                    }

                    @Override
                    public void onComplete() {
                        LogUtils.i("onComplete:");
                    }
                });
    }

    /**
     * 返回指定位置的数据
     */
    private void elementAt() {
        int a = Flowable.range(1, 10).elementAt(5, 0).blockingGet();
        LogUtils.i("elementAt:" + a);  //6
    }

    private void first() {
        //elementAt(0, defaultItem)
        int a = Flowable
                .range(1, 10)
                .first(-1)
                .blockingGet();
        LogUtils.i("elementAt:" + a);  //1
    }

    /**
     * 防抖
     * <em>NOTE:</em>如果在设定好的时间结束前源Observable有新的数据发射出来，
     * 这个数据就会被丢弃，同时重新开始计时
     * 可用于editview一定时间没有操作，自动搜素
     */
    private void debounce() {
        Disposable d = Flowable.create(emitter -> {
//            for (int i = 0; i < 10; i++) {
//                emitter.onNext(i);
//                SystemClock.sleep(100 * i);
//            }
            //发射第1个数后，休眠了400ms,不满足500ms,被过滤掉
            emitter.onNext(1);
            SystemClock.sleep(400);
            //再发射第2个数据，休眠200ms,也不满足500ms
            emitter.onNext(2);
            SystemClock.sleep(200);
            //再发射第3个数据，不满足500ms
            emitter.onNext(3);
            //发射第4个数据，满足500ms后发射数据
            emitter.onNext(4);
            SystemClock.sleep(550);
            //发射第5个数据
            emitter.onNext(5);
            emitter.onComplete();
        }, BackpressureStrategy.DROP)
                .debounce(500, TimeUnit.MILLISECONDS)  //如果发射数据间隔少于等于500ms就过滤拦截掉
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(value -> LogUtils.i("debounce:" + value)); //4，5
    }

    /**
     * 当Flowable遇到错误的时候，不调用onError而是发射一个数据序列
     */
    private void onErrorResumeNext() {
        Flowable<String> error = Flowable.create(emitter -> {
            emitter.onError(new Throwable("test error"));
            emitter.onComplete();
        }, BackpressureStrategy.DROP);
        Disposable d = error.onErrorResumeNext(new Function<Throwable, Publisher<String>>() {
            @Override
            public Publisher<String> apply(Throwable throwable) throws Exception {
                print(throwable);
                return Flowable.just("1");
            }
        }).subscribe(this::print);  //1
    }

    private void onErrorReturn() {
        Flowable<String> error = Flowable.create(emitter -> {
            emitter.onError(new Throwable("test error"));
            emitter.onComplete();
        }, BackpressureStrategy.DROP);
        Disposable d = error
                .onErrorReturn(new Function<Throwable, String>() {
                    @Override
                    public String apply(Throwable throwable) throws Exception {
                        return "one";
                    }
                })
                .subscribe(this::print);  //one
        Disposable d1 = error
                .onErrorReturnItem("two")
                .subscribe(this::print); //two
    }

    /**
     * 将一个普通的Flowable对象转化为一个Connectable Flowable
     * <p>
     * NOTE:ConnectableFlowable并不是subscribe的时候发射数据，而是只有对其应用connect操作符的时候才开始发射数据
     * 如果发射数据已经开始了再进行订阅只能接收以后发射的数据。
     */
    private void publish() {
        ConnectableFlowable<Integer> connetc = Flowable.range(1, 100).publish();
        Disposable d = connetc.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                LogUtils.e("publish:" + integer);
            }
        });

        connetc.connect();

    }

    /**
     * 重复源Publisher发出的项目序列times次。
     */
    private void repeat() {
        Disposable d = Flowable.just(1, 2, 3)
                .repeat(10)
                .subscribe(this::print);
    }

    /**
     * 遇到错误时重试
     * <em>NOTE:</em> count:失败前的重试次数 加上第一次发射的总共是count+1次
     * 最后一次重试,emitter.onError之后会继续运行程序，但emitter.onError后面的数据不会发射
     */
    private void retry() {
        Flowable<String> error = Flowable.create(emitter -> {
            for (int i = 0; i < 10; i++) {
                if (i == 5) {
                    emitter.onError(new Throwable("error test"));
                } else {
                    emitter.onNext(String.valueOf(i));
                }
                SystemClock.sleep(300);
                LogUtils.e("#:" + Thread.currentThread().getName());
            }
            emitter.onComplete();
        }, BackpressureStrategy.DROP);

        Disposable d = error.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.io())
                //重试结束标准：次数到达或返回false，以先发生者为准。
                .retry(2, new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable throwable) throws Exception {
                        return false;
                    }
                })
                .subscribe(value -> LogUtils.i("retry:" + value), this::print);
    }

    /**
     * 在周期时间间隔内，发射Flowable发射的最新数据
     */
    private void sample() {
        Disposable d = Flowable.intervalRange(1, 10, 0, 1000, TimeUnit.MILLISECONDS)
                .sample(2, TimeUnit.SECONDS, false)  ////每2秒采集最后一个元素
                .subscribe(this::print);  //2、4、6、8，之后会直接触发onComplete事件。
    }

    /**
     * 1——2——3——4——5——6——7——8——9——10
     * 如果在指定的时间内发射了多个数据，取这段时间内第一个发射的数据发射
     * 可以理解为发射可一堆数据，拿着一个指定长度东西来比照，这个范围内的发射第一个，然后再在剩下的数据里面依次比较
     */
    private void throttleFirst() {
        Disposable d = Flowable
                .intervalRange(1, 10, 0, 1500, TimeUnit.MILLISECONDS)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe(this::print);  //1,3,5,7,9
    }


    private void throttleLast(){
        Disposable d = Flowable.intervalRange(1, 10, 0, 1000, TimeUnit.MILLISECONDS)
                .throttleLast(2, TimeUnit.SECONDS, Schedulers.computation())  ////每2秒采集最后一个元素
                .subscribe(this::print);  //2、4、6、8
    }

    /**
     *第一个item不计入计时，从第二个item开始去周期时间内最新发射的item
     *
     */
    private void throttleLatest(){
        Disposable d = Flowable.intervalRange(1, 10, 900, 900, TimeUnit.MILLISECONDS)
                .throttleLatest(2, TimeUnit.SECONDS, Schedulers.computation(),true)  ////每2秒采集最后一个元素
                .subscribe(this::print);  //1,3,5,7,9,10
    }
    /**
     * 返回一个ConnectableFlowable，它共享对基础Publisher的单个订阅，他将重播所有的的items给任何未来的订阅。、
     * 即再次订阅 会对这个订阅的重复播放一遍
     */
    private void replay() {
        //缓存数据源的个数，凡是在connect()之后订阅的观察者，只能收到缓存的数据，connect()之前订阅的可以收到所有数据。
        // 即replay()缓存了所有数据源，而replay(int buffersize)缓存指定个数的数据源。
        ConnectableFlowable<Integer> connect = Flowable.just(1, 2, 3).replay(2);
        Disposable d = connect.subscribe(this::print);
        connect.connect(); //1,2,3  ConnectableFlowable在connect时开始发射数据

        LogUtils.i("订阅2：");
        SystemClock.sleep(1000);
        connect.subscribe(this::print);  //2,3
    }

    private void replay1() {
        //缓存2个数据，保留缓存1秒钟
        ConnectableFlowable<Integer> connect = Flowable
                .just(1, 2, 3)
                .replay(2, 1000, TimeUnit.MILLISECONDS, Schedulers.io());
        Disposable d = connect.subscribe(this::print);
        connect.connect(); //1,2,3

        LogUtils.i("订阅2：");
        SystemClock.sleep(500);
        connect.subscribe(this::print);  //2,3

        LogUtils.i("订阅3：");
        SystemClock.sleep(1000);
        connect.subscribe(this::print);  //
    }

    /**
     * 排序
     */
    private void sorted() {
        Disposable d = Flowable.just(9, 4, 2, 8, 1, 3)
                .sorted((o1, o2) -> o1 - o2)
                .subscribe(this::print);
    }

    /**
     * 只发射开始的N项数据
     */
    private void take() {
        Flowable.range(1, 10)
                .take(6)
                .subscribe(this::print);
    }

    private <T> void print(T t) {
        LogUtils.e("#" + t.getClass().getSimpleName() + ":" + t);
    }
}

