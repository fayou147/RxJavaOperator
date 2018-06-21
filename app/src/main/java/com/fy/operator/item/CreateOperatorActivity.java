package com.fy.operator.item;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.fy.operator.LogUtils;
import com.fy.operator.R;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class CreateOperatorActivity extends AppCompatActivity {
    private static final String TAG = "CreateOperatorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_operator);
//        create();
        intervalRange();
    }

    private void create() {
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(10);
                emitter.onComplete();
            }
        }, BackpressureStrategy.BUFFER).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                Log.e(TAG, "onSubscribe" + s);
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "integer:" + integer);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });
    }


    /**
     * 延迟
     * <p></p>
     * <b>NOTE:</b>
     * 创建类操作符。do not create the Observable until the observer subscribes, and create a fresh Observable for each observer
     * 不会创建Observable直到观察者订阅了被观察者，并且订阅时创建一个新的被观察者。即：订阅时才会创建Observable
     */
    private int deferIndex = 10;

    private void defer() {
        Flowable<Integer> flowable = Flowable.defer(new Callable<Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> call() throws Exception {
                return Flowable.just(deferIndex);
            }
        });
        deferIndex = 20;
        flowable.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.i(TAG, "accept:" + integer);  //20
            }
        });
    }

    /**
     * 不发射任何item,只调用onComplete方法
     * <p></p>
     * <b>NOTE:</b>
     * 创建类操作符.创建一个发射任何item,只调用onComplete的被观察者
     */
    private void empty() {
        Flowable<Integer> flowable = Flowable.empty();
        flowable.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Integer integer) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });
    }

    /**
     * <p></p>
     * <b>NOTE:</b>
     * <p>
     * 创建类操作符,只调用onError方法
     */
    private void error() {
//        Flowable<Integer> flowable = Flowable.error(new Throwable("unknown error"));
        Flowable<Integer> flowable = Flowable.error(new Callable<Throwable>() {
            @Override
            public Throwable call() throws Exception {
                return new Throwable("unknown error");
            }
        });
        flowable.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Integer integer) {

            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Throwable:" + t);
            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void fromArray() {
        Flowable.fromArray(1, 2, 3, 4).subscribe(new Subscriber<Integer>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
                this.s = s;
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "onNext:" + integer);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                Log.i(TAG, "onError:" + t);
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });

    }

    /**
     * <p></p>
     * <b>NOTE:</b>
     * 订阅者订阅时，返回一个Flowable，调用指定的函数，然后发射该函数返回值。
     */
    private int fromCallableIndex = 10;

    private void fromCallable() {
        Flowable<Integer> flowable = Flowable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return fromCallableIndex;
            }
        });
        fromCallableIndex = 20;
        flowable.subscribe(new FlowableSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "fromCallable:" + integer);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }


    private int justIndex = 10;

    private void just() {
        Flowable<Integer> flowable = Flowable.just(justIndex);
        justIndex = 20;
        flowable.subscribe(new FlowableSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "just:" + integer);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * <P></P>
     * <b>NOTE:</b>
     * <p>
     * Future模式。可以在子线程运行完成后有return返回并发射，Future并不是子线程接口，它和Runnable类似，只不过Runnable没有返回值，Future.call有返回值
     */
    private void fromFuture() {
        //futurePublisher.doOnCancel(() -> future.cancel(true));
        /**
         * Future类似Runnable,本身不是线程，两者区别Runnable没有返回值，Future.call有返回值
         */
        // 1
        final ExecutorService service = Executors.newCachedThreadPool();
        final Future<Integer> task = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                SystemClock.sleep(5000);
                Log.i(TAG, "" + Thread.currentThread().toString());
                return 10;
            }
        });

        Flowable.fromFuture(task).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "fromFuture:" + integer);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        //2
        final FutureTask<Integer> task2 = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                SystemClock.sleep(10000);
                Log.i(TAG, "" + Thread.currentThread().toString());
                return 10;
            }
        });

        Flowable.fromFuture(task2).doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription subscription) throws Exception {
                subscription.request(1);
                //此方法作用在子线程时因为subscribeOn在子线程，否则是在主线程运行
                task2.run();
            }
        }).subscribeOn(Schedulers.newThread()).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Integer integer) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * 将一个Iterable序列转变成为一个Publisher，发射序列中的item
     */
    private void fromIterable() {
        List<Integer> arrayInt = Arrays.asList(1, 2, 3, 4, 5);
        Flowable.fromIterable(arrayInt).subscribe(new FlowableSubscriber<Integer>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                s.request(1);
                Log.i(TAG, "fromIterable:" + integer);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * 将任意Reactive-Streams(响应流)Publisher转换成Flowable
     */
    private void fromPublisher() {
        Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onNext(1);
                s.onComplete();
            }
        }).subscribe(new FlowableSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "fromPublisher:" + integer);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void generate() {
        Flowable.generate(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return null;
            }
        }, new BiConsumer<String, Emitter<Integer>>() {
            @Override
            public void accept(String s, Emitter<Integer> integerEmitter) throws Exception {

            }
        });


        Flowable.generate(new Consumer<Emitter<Integer>>() {
            @Override
            public void accept(Emitter<Integer> integerEmitter) throws Exception {
                integerEmitter.onNext(1);
                integerEmitter.onComplete();
            }
        }).subscribe(new FlowableSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Integer integer) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * @param initialDelay 发射第一个值等待的延迟时间
     * @param period       序列数组发射的时间间隔
     */
    private void interval() {
        Flowable.interval(3000, 1000, TimeUnit.MILLISECONDS).subscribe(new Subscriber<Long>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Long aLong) {
                Log.i(TAG, "interval':" + aLong);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * <p>最后一个值(start + count - 1)到达后立即调用onComplete</p>
     * <b>NOTE:</b>
     */
    private void intervalRange() {
        Flowable.intervalRange(0, 10, 3000, 1000, TimeUnit.MILLISECONDS).subscribe(new FlowableSubscriber<Long>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Long aLong) {
                Log.i(TAG, "intervalRange:" + aLong);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void range() {
        Disposable d = Flowable.range(0, 10).subscribe(value -> LogUtils.i("value:" + value));
    }

    private void timer() {
        Flowable.just(111).timer(1000, TimeUnit.MILLISECONDS).subscribe(value -> LogUtils.i("value:" + value));
    }
}
